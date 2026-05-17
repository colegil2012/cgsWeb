package com.ua.estore.cgsWeb.models.dto.admin;

import com.ua.estore.cgsWeb.models.vendor.Vendor;
import lombok.Builder;
import lombok.Getter;

/**
 * Flat projection of {@link Vendor} for the admin vendor-list view.
 *
 * <p>Carries a {@code productCount} the list query fills in per row — the
 * Vendor document doesn't store it (products point at vendors, not the
 * reverse), so the service does a count per vendor when building the list.</p>
 */
@Getter
@Builder
public class AdminVendorListItemDTO {

    private String id;
    private String name;
    private String slug;
    private boolean active;
    private long productCount;

    public static AdminVendorListItemDTO from(Vendor vendor, long productCount) {
        return AdminVendorListItemDTO.builder()
                .id(vendor.getId())
                .name(vendor.getName())
                .slug(vendor.getSlug())
                .active(vendor.isActive())
                .productCount(productCount)
                .build();
    }
}