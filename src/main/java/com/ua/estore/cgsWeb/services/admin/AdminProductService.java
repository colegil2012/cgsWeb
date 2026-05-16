package com.ua.estore.cgsWeb.services.admin;

import com.ua.estore.cgsWeb.models.dto.admin.AdminProductListItemDTO;
import com.ua.estore.cgsWeb.models.shop.Product;
import com.ua.estore.cgsWeb.models.vendor.Vendor;
import com.ua.estore.cgsWeb.repositories.shop.ProductRepository;
import com.ua.estore.cgsWeb.repositories.vendor.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Admin-side product management.
 *
 * <p>The admin product views deliberately show <b>all</b> products
 * regardless of stock — the whole point of the inventory workflow is to see
 * 0-stock items so they can be restocked. The {@code stock > 0} filter that
 * hides products lives only on the storefront-facing repository queries.</p>
 *
 * <p>Products are listed either globally or scoped to one vendor (the
 * vendor detail page links through with {@code ?vendor=<id>}).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductService {

    private static final int PAGE_SIZE = 50;

    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;

    /* ============================================================================
     * Read
     * ============================================================================ */

    /**
     * Product list, optionally scoped to one vendor.
     *
     * @param vendorId null/blank = all products; otherwise that vendor's only
     */
    public PagedProductResult list(String vendorId, int page) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page), PAGE_SIZE, Sort.by("name").ascending());

        Page<Product> productPage;
        boolean scoped = vendorId != null && !vendorId.isBlank();
        if (scoped) {
            productPage = productRepository.findByVendorId(vendorId, pageable);
        } else {
            productPage = productRepository.findAll(pageable);
        }

        // Resolve vendor names in one batch rather than per-row.
        Map<String, String> vendorNames = resolveVendorNames(productPage.getContent());

        List<AdminProductListItemDTO> items = productPage.getContent().stream()
                .map(p -> AdminProductListItemDTO.from(
                        p, vendorNames.get(p.getVendorId())))
                .toList();

        return new PagedProductResult(
                items, productPage.getNumber(), productPage.getTotalPages(),
                productPage.getTotalElements(), scoped ? vendorId : null);
    }

    public Optional<Product> findById(String productId) {
        if (productId == null || productId.isBlank()) return Optional.empty();
        return productRepository.findById(productId);
    }

    /* ============================================================================
     * Create / update
     * ============================================================================ */

    public Product createProduct(String name, String sku, String description,
                                 BigDecimal price, BigDecimal salePrice,
                                 Integer stock, Integer lowStockThreshold,
                                 String imageUrl, boolean active,
                                 String vendorId, String slugOverride,
                                 String categoryId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (vendorId == null || vendorId.isBlank()) {
            throw new IllegalArgumentException("A vendor must be selected.");
        }
        if (vendorRepository.findById(vendorId).isEmpty()) {
            throw new IllegalArgumentException("Selected vendor does not exist.");
        }

        Product p = new Product();
        p.setName(name.trim());
        p.setSku(trimOrNull(sku));
        p.setDescription(trimOrNull(description));
        p.setPrice(price);
        p.setSalePrice(salePrice);
        p.setStock(stock == null ? 0 : stock);
        p.setLowStockThreshold(lowStockThreshold);
        p.setImageUrl(trimOrNull(imageUrl));
        p.setActive(active);
        p.setVendorId(vendorId);
        p.setCategoryId(trimOrNull(categoryId));
        p.setSlug(resolveSlug(slugOverride, name, null));
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());

        Product saved = productRepository.save(p);
        log.info("Product created: id={} name={} vendor={}",
                saved.getId(), saved.getName(), vendorId);
        return saved;
    }

    public Product updateProduct(String productId, String name, String sku,
                                 String description, BigDecimal price,
                                 BigDecimal salePrice, Integer stock,
                                 Integer lowStockThreshold, String imageUrl,
                                 boolean active, String slugOverride,
                                 String categoryId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        p.setName(name.trim());
        p.setSku(trimOrNull(sku));
        p.setDescription(trimOrNull(description));
        p.setPrice(price);
        p.setSalePrice(salePrice);
        if (stock != null) p.setStock(stock);
        p.setLowStockThreshold(lowStockThreshold);
        p.setImageUrl(trimOrNull(imageUrl));
        p.setActive(active);
        p.setCategoryId(trimOrNull(categoryId));
        p.setSlug(resolveSlug(slugOverride, name, productId));
        if (p.getCreatedAt() == null) p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());

        Product saved = productRepository.save(p);
        log.info("Product updated: id={}", saved.getId());
        return saved;
    }

    /**
     * Set a product's stock to an explicit value. The dedicated "restock"
     * action from the product list / detail. Sets rather than increments —
     * avoids the null-unbox NPE in Product.increaseStock() and matches the
     * "set quantity of the item" workflow.
     */
    public Product setStock(String productId, int newStock) {
        if (newStock < 0) {
            throw new IllegalArgumentException("Stock can't be negative.");
        }
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        p.setStock(newStock);
        p.setUpdatedAt(LocalDateTime.now());
        Product saved = productRepository.save(p);
        log.info("Product {} stock set to {}", productId, newStock);
        return saved;
    }

    /* ============================================================================
     * Helpers
     * ============================================================================ */

    private Map<String, String> resolveVendorNames(List<Product> products) {
        Map<String, String> names = new HashMap<>();
        for (Product p : products) {
            String vid = p.getVendorId();
            if (vid == null || names.containsKey(vid)) continue;
            vendorRepository.findById(vid)
                    .ifPresent(v -> names.put(vid, v.getName()));
        }
        return names;
    }

    private String resolveSlug(String slugOverride, String name, String excludeProductId) {
        String base = (slugOverride != null && !slugOverride.isBlank())
                ? AdminVendorService.slugify(slugOverride)
                : AdminVendorService.slugify(name);
        if (base.isBlank()) base = "product";

        String candidate = base;
        int suffix = 2;
        while (slugTaken(candidate, excludeProductId)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private boolean slugTaken(String slug, String excludeProductId) {
        Optional<Product> existing = productRepository.findBySlug(slug);
        if (existing.isEmpty()) return false;
        return excludeProductId == null || !existing.get().getId().equals(excludeProductId);
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /* ============================================================================
     * Result type
     * ============================================================================ */

    public record PagedProductResult(
            List<AdminProductListItemDTO> items,
            int page,
            int totalPages,
            long totalElements,
            String vendorFilter        // the vendor id if scoped, else null
    ) {}
}