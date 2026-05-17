package com.ua.estore.cgsWeb.models.dto.admin;

import com.ua.estore.cgsWeb.models.shop.Product;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Flat projection of {@link Product} for the admin product-list view.
 *
 * <p>Computes a {@code stockStatus} string so the template can render a
 * badge without logic: {@code OUT} (stock 0 or null), {@code LOW} (at or
 * below the low-stock threshold), {@code OK} otherwise.</p>
 */
@Getter
@Builder
public class AdminProductListItemDTO {

    private String id;
    private String name;
    private String sku;
    private BigDecimal price;
    private Integer stock;
    private boolean active;
    private String vendorId;
    private String vendorName;       // resolved by the service via a vendor lookup
    private String stockStatus;      // OUT | LOW | OK

    public static AdminProductListItemDTO from(Product p, String vendorName) {
        return AdminProductListItemDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .sku(p.getSku())
                .price(p.getPrice())
                .stock(p.getStock())
                .active(p.isActive())
                .vendorId(p.getVendorId())
                .vendorName(vendorName)
                .stockStatus(computeStockStatus(p))
                .build();
    }

    private static String computeStockStatus(Product p) {
        Integer stock = p.getStock();
        if (stock == null || stock <= 0) return "OUT";

        Integer threshold = p.getLowStockThreshold();
        if (threshold != null && stock <= threshold) return "LOW";

        return "OK";
    }
}