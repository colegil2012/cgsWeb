package com.ua.estore.cgsWeb.services.shop;

import com.ua.estore.cgsWeb.models.shop.Order;
import com.ua.estore.cgsWeb.models.shop.Product;
import com.ua.estore.cgsWeb.repositories.shop.ProductRepository;
import com.ua.estore.cgsWeb.util.dataUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
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
     * Inventory — stock decrement / restock
     *
     * Called when an order changes state:
     *   - decrementStockForOrder: order becomes PAID (stock consumed)
     *   - restockForOrder:        a paid order is cancelled (stock returned)
     *
     * Both are deliberately fault-tolerant. Stock bookkeeping must NEVER
     * fail the order operation it's attached to — a missing product or a
     * malformed line item is logged and skipped, and the loop continues.
     * The order save has already happened (or is about to); inventory is
     * best-effort consistency on top of it.
     *******************************************/

    /**
     * Decrement stock for every line item on the given order.
     *
     * <p>Stock is clamped at zero — an order whose quantity exceeds the
     * product's on-hand count drives stock to 0, never negative. The
     * storefront's {@code stock > 0} filter then simply hides the product
     * until it's restocked.</p>
     *
     * <p>Fault-tolerant: a line item whose product can't be found, or whose
     * fields are malformed, is logged at WARN and skipped. This method does
     * not throw.</p>
     */
    public void decrementStockForOrder(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        for (Order.OrderItem item : order.getItems()) {
            applyStockDelta(order, item, -quantityOf(item));
        }
    }

    /**
     * Return stock for every line item on the given order — the inverse of
     * {@link #decrementStockForOrder}. Used when a paid order is cancelled.
     *
     * <p>Same fault tolerance as the decrement path.</p>
     */
    public void restockForOrder(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        for (Order.OrderItem item : order.getItems()) {
            applyStockDelta(order, item, +quantityOf(item));
        }
    }

    /**
     * Apply a signed stock change for a single order line item.
     *
     * <p>{@code delta} is negative to consume stock, positive to return it.
     * The new stock value is clamped so it never goes below zero; a null
     * current stock is treated as zero (which also sidesteps the NPE in
     * {@code Product.increaseStock()/decreaseStock()} — we never call those,
     * we compute and set explicitly).</p>
     *
     * <p>Any failure here is contained: logged, then swallowed, so a single
     * bad line item can't abort the rest of the order's stock updates or
     * the order operation itself.</p>
     */
    private void applyStockDelta(Order order, Order.OrderItem item, int delta) {
        String orderNumber = (order != null) ? order.getOrderNumber() : "(unknown)";
        try {
            if (item == null || item.getProductId() == null || item.getProductId().isBlank()) {
                log.warn("Stock update skipped for order {} — line item missing productId", orderNumber);
                return;
            }
            if (delta == 0) {
                return;   // zero-quantity line — nothing to do
            }

            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product == null) {
                log.warn("Stock update skipped for order {} — product {} not found",
                        orderNumber, item.getProductId());
                return;
            }

            int current = (product.getStock() != null) ? product.getStock() : 0;
            int updated = current + delta;
            if (updated < 0) updated = 0;   // clamp — never negative

            product.setStock(updated);
            productRepository.save(product);

            log.info("Stock {} for product {} (order {}): {} -> {}",
                    delta < 0 ? "decremented" : "restocked",
                    product.getId(), orderNumber, current, updated);

        } catch (Exception ex) {
            // Never let a stock bookkeeping failure escape — the order
            // operation this is attached to must complete regardless.
            log.warn("Stock update failed for order {} item {} — skipped: {}",
                    orderNumber,
                    (item != null ? item.getProductId() : "(null)"),
                    ex.getMessage());
        }
    }

    /** Null-safe quantity read. A null or absent quantity counts as zero. */
    private static int quantityOf(Order.OrderItem item) {
        if (item == null || item.getQuantity() == null) return 0;
        return item.getQuantity();
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