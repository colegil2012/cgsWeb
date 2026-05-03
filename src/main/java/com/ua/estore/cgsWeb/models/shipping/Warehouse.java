package com.ua.estore.cgsWeb.models.shipping;

import com.ua.estore.cgsWeb.models.address.Address;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * A physical origin point from which deliveries are dispatched.
 *
 * <p>Phase 1 (single-origin): exactly one warehouse is marked {@code primary = true}
 * and {@code active = true}; every delivery ships from it.</p>
 *
 * <p>Future (multi-origin): products will carry a {@code fulfillmentWarehouseId} and
 * the shipping service will group cart items by warehouse, pricing each leg
 * independently. No schema change is required to get there.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "warehouses")
public class Warehouse {

    @Id
    private String id;
    private String name;

    /** Short slug for admin URLs, e.g. "celtech-bloomfield". */
    private String slug;

    /** The warehouse's physical address. Reuses the existing Address model for consistency. */
    private Address address;

    /** Exactly one active warehouse should be {@code primary} in Phase 1. */
    private boolean primary;

    /** Soft on/off switch — inactive warehouses are ignored by the shipping service. */
    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}