
package com.ua.estore.cgsWeb.models.shop;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The financial record of a customer purchase. Created at checkout, mutated by Square
 * webhooks and refunds, but otherwise immutable.
 *
 * <p><b>Snapshot model:</b> customer name/phone, addresses, and product names/prices
 * are <em>copied</em> into the order at the moment of checkout. The user/product/vendor
 * documents may be referenced (by id) for traceability, but never followed at read time
 * — that way a price change or renamed user doesn't retroactively rewrite history.</p>
 *
 * <p><b>Two status fields:</b></p>
 * <ul>
 *   <li>{@link #status} – financial lifecycle (PENDING → PAID → REFUNDED).</li>
 *   <li>The matching {@code Delivery} doc carries the logistical lifecycle so the
 *       driver app can update fulfillment without touching the order.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    /** Pointer back to the buyer (do NOT dereference for display – use {@link #customer}). */
    @Indexed
    @Field(targetType = FieldType.OBJECT_ID)
    private String userId;

    @Indexed(unique = true, sparse = true)
    private String orderNumber;

    /** idempotency key – also doubles as our retry guard for the whole submit flow. */
    @Indexed(unique = true, sparse = true)
    private String idempotencyKey;

    private OrderStatus   status;        // PENDING, PAID, CANCELLED, REFUNDED
    private PaymentStatus paymentStatus; // NOT_ATTEMPTED, AUTHORIZED, CAPTURED, FAILED

    private CustomerSnapshot customer;   // name+email+phone copied at checkout
    private AddressSnapshot  shipTo;     // full address copy + coords
    private AddressSnapshot  billTo;     // optional, for receipts

    private OrderTotals totals;
    private List<OrderItem> items = new ArrayList<>();

    private String deliveryInstructions;

    /** Optional user reference; passed straight through to Square if present. */
    private String customerNote;

    private LocalDateTime placedAt;      // when checkout submit succeeded
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;   // null unless status == CANCELLED
    private LocalDateTime refundedAt;    // null unless status == REFUNDED
    private LocalDateTime deliveredAt;

    /* ============================================================================
     * Embedded snapshot types
     * ============================================================================ */

    @Data
    @NoArgsConstructor
    public static class CustomerSnapshot {
        @Field(targetType = FieldType.OBJECT_ID)
        private String userId;            // pointer for joins / reports only
        private String firstName;
        private String lastName;
        private String email;
        /** E.164 (e.g. "+15025551234") preferred for SMS later; free-form for now. */
        private String phone;
    }

    @Data
    @NoArgsConstructor
    public static class AddressSnapshot {
        /** The Address.addressId from the source user/vendor doc, for traceability. */
        private String sourceAddressId;
        private String type;              // SHIPPING / BILLING / ALTERNATE
        private String street1;
        private String street2;
        private String city;
        private String state;
        private String zip;
        private double latitude;
        private double longitude;

        private GeoPoint location;

        /** Re-derive {@link #location} from {@link #latitude}/{@link #longitude}. */
        public void syncGeoPoint() {
            this.location = new GeoPoint(longitude, latitude);
        }
    }

    /** Minimal GeoJSON Point – Mongo natively understands {@code type: "Point"}. */
    @Data
    @NoArgsConstructor
    public static class GeoPoint {
        private final String type = "Point";
        private double[] coordinates; // [lng, lat]

        public GeoPoint(double lng, double lat) {
            this.coordinates = new double[]{lng, lat};
        }
    }

    @Data
    @NoArgsConstructor
    public static class OrderTotals {
        private BigDecimal subtotal;
        private BigDecimal tax;
        private BigDecimal shipping;
        private BigDecimal discount;     // promotions/coupons – defaults to ZERO
        private BigDecimal total;
        /** Snapshot of which RateCard.id priced this order (matches your Phase 1 plan). */
        private String rateCardId;
    }

    /**
     * One line item, fully snapshotted. Driver paperwork reads from these fields ONLY;
     * the live product can disappear without breaking historical orders.
     */
    @Data
    @NoArgsConstructor
    public static class OrderItem {
        @Field(targetType = FieldType.OBJECT_ID)
        private String productId;

        @Field(targetType = FieldType.OBJECT_ID)
        private String vendorId;

        private String name;             // snapshot — "Organic basil 4oz"
        private String vendorName;       // snapshot — "Greenhill Farms"
        private String imageUrl;         // snapshot — for emailed receipts
        private String sku;              // optional, for inventory reconciliation

        private BigDecimal priceAtPurchase;
        private BigDecimal lineTotal;    // priceAtPurchase * quantity (denormalized)
        private Integer quantity;

        /** Free text from the customer, e.g. "ripe please". Per-line scope. */
        private String itemNote;
    }

    /** Financial state machine. */
    public enum OrderStatus { PENDING, PAID, CANCELLED, REFUNDED }

    /** Where the actual money is in the Square pipeline. */
    public enum PaymentStatus { NOT_ATTEMPTED, AUTHORIZED, CAPTURED, FAILED }
}