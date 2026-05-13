package com.ua.estore.cgsWeb.models.driver.delivery;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The logistical lifecycle record for a single order's fulfillment.
 *
 * <p><b>Single source of truth for delivery status.</b> When the driver marks an
 * order delivered, this document's {@link #status} flips — and that is the only
 * place delivery state lives. Routes reference Deliveries by id; they do not
 * carry their own status fields. This keeps the data model consistent and means
 * we never have two records disagreeing about whether a customer was served.</p>
 *
 * <p><b>Snapshot model:</b> mirrors the pattern on {@code Order}. Customer name,
 * address, and delivery instructions are copied in at creation time. If the
 * underlying {@code Order} or its source User/Address ever changed mid-flight
 * (which it shouldn't, but defensively), the in-flight Delivery is unaffected.</p>
 *
 * <p><b>Creation:</b> currently lazy — the driver service creates a Delivery
 * the first time it encounters an Order without one. Long-term this should
 * move to an event hook on Square payment success (so Deliveries always exist
 * for PAID orders without driver-app intervention).</p>
 *
 * <p><b>Order propagation TODO:</b> when {@link #status} transitions to
 * {@link DeliveryStatus#DELIVERED}, we should denormalize a {@code deliveredAt}
 * timestamp onto the source {@code Order} so the storefront's "Your Orders"
 * page can display "Delivered Nov 12" without a join. Wire this up in
 * {@code DeliveryService} when the status-update endpoint is built.</p>
 */
@Data
@NoArgsConstructor
@Document(collection = "deliveries")
public class Delivery {

    @Id
    private String id;

    /**
     * Pointer to the source Order. Unique — one Delivery per Order. Indexed
     * because the driver service looks up "is there a Delivery for this Order"
     * on every orders-list call.
     */
    @Indexed(unique = true)
    @Field(targetType = FieldType.OBJECT_ID)
    private String orderId;

    private String orderNumber;
    private CustomerSnapshot customer;
    private AddressSnapshot address;
    private String deliveryInstructions;

    /** Current logistical state. Source of truth — nothing else stores this. */
    private DeliveryStatus status;

    /**
     * If this Delivery is currently assigned to a Route, the Route's id. Null
     * otherwise. Updated when a Route is generated (set) or cancelled (cleared).
     * Lets us answer "is this Delivery on a planned route" without scanning
     * all Routes.
     */
    @Indexed(sparse = true)
    @Field(targetType = FieldType.OBJECT_ID)
    private String currentRouteId;

    /**
     * Future multi-driver hook. Null in owner-operator phase. When non-null,
     * indicates which driver owns this delivery's fulfillment.
     */
    @Field(targetType = FieldType.OBJECT_ID)
    private String assignedDriverId;

    /**
     * Full attempt history. The current {@link #status} reflects the latest
     * attempt's outcome; this list is the audit trail. A failed first attempt
     * + successful second attempt produces two entries here and a final
     * status of {@link DeliveryStatus#DELIVERED}.
     */
    private List<DeliveryAttempt> attempts = new ArrayList<>();

    /** Last note left by a driver, surfaced for quick reference. */
    private String driverNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Set when {@link #status} transitions to DELIVERED. Null otherwise. */
    private LocalDateTime deliveredAt;

    /* ============================================================================
     * Embedded snapshot types
     * ============================================================================ */

    /** Frozen-at-creation view of the customer. Mirrors Order.CustomerSnapshot. */
    @Data
    @NoArgsConstructor
    public static class CustomerSnapshot {
        @Field(targetType = FieldType.OBJECT_ID)
        private String userId;
        private String firstName;
        private String lastName;
        private String email;
        /** E.164 preferred for SMS later; free-form for now. */
        private String phone;
    }

    /**
     * Frozen-at-creation copy of the shipping address + GeoJSON Point.
     * Functionally identical to Order.AddressSnapshot — duplicated here rather
     * than shared so the two snapshots can drift if their semantics ever
     * diverge (Delivery might gain "preferred drop zone within property" later).
     */
    @Data
    @NoArgsConstructor
    public static class AddressSnapshot {
        private String sourceAddressId;
        private String street1;
        private String street2;
        private String city;
        private String state;
        private String zip;
        private double latitude;
        private double longitude;

        /** GeoJSON Point — {@code { type: "Point", coordinates: [lng, lat] }}. */
        private GeoPoint location;

        /** Re-derive {@link #location} from {@link #latitude}/{@link #longitude}. */
        public void syncGeoPoint() {
            this.location = new GeoPoint(longitude, latitude);
        }
    }

    /**
     * Minimal GeoJSON Point. Mirrors the shape on Order.GeoPoint so the geo
     * index machinery is identical for both collections.
     */
    @Data
    @NoArgsConstructor
    public static class GeoPoint {
        private final String type = "Point";
        private double[] coordinates;

        public GeoPoint(double lng, double lat) {
            this.coordinates = new double[]{lng, lat};
        }
    }
}