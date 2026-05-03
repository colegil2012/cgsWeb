package com.ua.estore.cgsWeb.services.shop;

import com.ua.estore.cgsWeb.models.shop.Order;
import com.ua.estore.cgsWeb.models.shop.Product;
import com.ua.estore.cgsWeb.repositories.shop.ProductRepository;
import com.ua.estore.cgsWeb.util.dataUtil;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;


    /********************************************
     * Save Methods
     *******************************************/

    public String saveProduct(Product product, String vendorId) {
        if (product == null) {
            throw new IllegalArgumentException("Product payload is missing.");
        }
        if (vendorId == null || vendorId.isBlank()) {
            throw new IllegalArgumentException("Vendor id is required.");
        }
        if (product.getCategoryId() == null || product.getCategoryId().isBlank()) {
            throw new IllegalArgumentException("Category is required.");
        }

        ObjectId cleanVendorId = dataUtil.parseToObjectId(vendorId);
        ObjectId cleanCatId = dataUtil.parseToObjectId(product.getCategoryId());

        if (cleanVendorId == null) {
            throw new IllegalArgumentException("Invalid vendor id.");
        }
        if (cleanCatId == null) {
            throw new IllegalArgumentException("Invalid category id.");
        }

        product.setVendorId(cleanVendorId.toHexString());
        product.setCategoryId(cleanCatId.toHexString());

        // B2C logic: Ensure active and generate a simple slug if missing
        product.setActive(true);
        if (product.getSlug() == null || product.getSlug().isEmpty()) {
            product.setSlug(product.getName().toLowerCase().replace(" ", "-") + "-" + System.currentTimeMillis() % 1000);
        }

        return productRepository.save(product).getId();
    }

    /********************************************
     * Get Methods
     *******************************************/

    public List<Product> recommendForOrder(Order order, int limit) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty() || limit <= 0) {
            return List.of();
        }

        Collection<String> vendorIds = order.getItems().stream()
                .map(Order.OrderItem::getVendorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Collection<String> excludeIds = order.getItems().stream()
                .map(Order.OrderItem::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (vendorIds.isEmpty()) return List.of();

        // Use insertion-ordered set so the dedup logic preserves "vendor first" priority.
        Set<Product> picks = new LinkedHashSet<>();

        // Pass 1: same-vendor.
        var vendorScoped = productRepository.findByVendorIdInAndIdNotInAndActiveTrue(
                vendorIds,
                excludeIds.isEmpty() ? List.of("__never__") : excludeIds,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        picks.addAll(vendorScoped);

        // Pass 2: top up with recent products from any vendor if we don't have enough.
        if (picks.size() < limit) {
            int needed = limit - picks.size();
            // Build a combined exclusion list: original exclusions + already-picked ids,
            // so we don't recommend the same product twice.
            Set<String> combinedExclude = new LinkedHashSet<>(excludeIds);
            picks.forEach(p -> combinedExclude.add(p.getId()));
            // PageRequest needs a non-empty exclude list to behave consistently.
            if (combinedExclude.isEmpty()) combinedExclude.add("__never__");

            var fallback = productRepository.findByIdNotInAndActiveTrueOrderByCreatedAtDesc(
                    combinedExclude,
                    PageRequest.of(0, needed)
            );
            picks.addAll(fallback);
        }

        return new ArrayList<>(picks);
    }

    /***********************************************
     * Singular
     ***********************************************/

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(String id) {
        return productRepository.findById(id).orElse(null);
    }

    public Page<Product> getProductsByFilter(String search, String category, String vendor, boolean lowStock, int page) {
        String searchQuery = (search == null) ? "" : search;
        Object categoryQuery = (category == null || category.isEmpty()) ? "" : dataUtil.parseToObjectId(category);
        Object vendorQuery = (vendor == null || vendor.isEmpty()) ? "" : dataUtil.parseToObjectId(vendor);

        var pageable = PageRequest.of(page, 16);

        return productRepository.findByFilter(categoryQuery, searchQuery, vendorQuery, lowStock, pageable);
    }

    public List<Product> getProductsByVendorId(String vendorId) {
        return productRepository.findByVendorId(vendorId);
    }

    public Page<Product> getProductsByVendorId(String vendorId, int page) {
        var pageable = PageRequest.of(page, 16);

        if (vendorId == null || vendorId.isEmpty()) return Page.empty(pageable);

        return productRepository.findByVendorId(vendorId, pageable);
    }
}
