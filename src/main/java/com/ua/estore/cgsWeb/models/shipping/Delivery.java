package com.ua.estore.cgsWeb.models.shipping;

import com.ua.estore.cgsWeb.models.shop.Order;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A physical delivery leg – the unit the driver dash app and route planner work with.
 *
 * <p>One {@link Order} → one {@link Delivery} in the
 * Phase-1 single-warehouse model. Splitting the two collections lets us later:</p>
 * <ul>
 *   <li>create multiple Deliveries per Order (partial shipments, back-orders),</li>
 *   <li>merge several Orders for the same customer into one Delivery,</li>
 *   <li>cancel/reschedule a Delivery without touching the financial record.</li>
 * </ul>
 *
 * <p>The driver dash polls/streams these documents; status transitions go through
 * a service so we can record a {@link StatusEvent} with timestamps + GPS for every
 * change.</p>
 */
@Data
@NoArgsConstructor
@Document(collection = "deliveries")
public class Delivery {

    @Id
    private String id;

    /** The Order being delivered. Many-to-one in future, one-to-one today. */
    @Indexed
    @Field(targetType = FieldType.OBJECT_ID)
    private String orderId;

    /** Origin (which warehouse this leg ships from). Always set. */
    @Indexed
    @Field(targetType = FieldType.OBJECT_ID)
    private String warehouseId;

    /** Destination, snapshotted exactly the way Order.shipTo is. */
    private Order.AddressSnapshot dropOff;

    /** Customer contact info copied from the Order so the driver doesn't need to look it up. */
    private Order.CustomerSnapshot customer;

    /** Compact list of what's being delivered, for the packing slip. */
    private List<DeliveryLine> lines = new ArrayList<>();

    /** Order total + payment posture, surfaced for COD / refused-delivery flows. */
    private BigDecimal orderTotal;
    private Order.PaymentStatus paymentStatus;

    /* ============================================================================
     * Scheduling / planner
     * ============================================================================ */

    private LocalDate scheduledDate;          // "deliver this on …"
    private TimeWindow window;                // optional earliest/latest within the day
    private Integer serviceDurationSec;       // expected dwell time at the stop
    private Integer plannedSequence;          // 1-based stop order within the route
    private Double  plannedDistanceMiles;     // from previous stop / warehouse
    private Integer plannedDurationSec;       // drive time from previous stop / warehouse

    @Field(targetType = FieldType.OBJECT_ID)
    private String routeId;                   // populated when assigned to a daily route

    @Field(targetType = FieldType.OBJECT_ID)
    private String driverId;                  // assigned driver (a User w/ role=DRIVER)

    /* ============================================================================
     * Live state
     * ============================================================================ */

    private DeliveryStatus status;            // see enum below
    private Instant arrivedAt;                // when driver pressed "arrived"
    private Instant deliveredAt;              // success
    private Instant failedAt;                 // refused / undeliverable

    /** Audit log: every status transition with who/when/where. Append-only. */
    private List<StatusEvent> statusHistory = new ArrayList<>();

    /** Proof of delivery captured by the driver app. */
    private ProofOfDelivery proofOfDelivery;

    /* ============================================================================
     * Special-handling flags. Default false; expand list as needed.
     * ============================================================================ */

    private boolean fragile;
    private boolean refrigerated;
    private boolean signatureRequired;
    private boolean leaveAtDoor;
    private String  driverInstructions;       // copied from Order.deliveryInstructions

    private Instant createdAt;
    private Instant updatedAt;

    /* ============================================================================
     * Embedded types
     * ============================================================================ */

    @Data
    @NoArgsConstructor
    public static class DeliveryLine {
        @Field(targetType = FieldType.OBJECT_ID)
        private String productId;
        private String name;
        private Integer quantity;
        private String itemNote;
    }

    @Data
    @NoArgsConstructor
    public static class TimeWindow {
        /** Earliest and latest times of day acceptable for delivery. ISO-8601 instants. */
        private Instant earliest;
        private Instant latest;
    }

    /**
     * One row of the audit log. Recorded for every status change.
     *
     * <p>{@code recordedAt} is server time; {@code reportedAt} is what the driver's
     * device claimed (used to detect clock skew).</p>
     */
    @Data
    @NoArgsConstructor
    public static class StatusEvent {
        private DeliveryStatus from;
        private DeliveryStatus to;
        private Instant recordedAt;
        private Instant reportedAt;
        private Order.GeoPoint location;       // optional – where the driver was
        private String idempotencyKey;         // dedupe replayed driver-app events
        private String note;                   // optional driver note
        @Field(targetType = FieldType.OBJECT_ID)
        private String byUserId;               // driver/admin who triggered it
    }

    @Data
    @NoArgsConstructor
    public static class ProofOfDelivery {
        /** Public URL or DigitalOcean Spaces key to the delivery photo. */
        private String photoUrl;
        /** Spaces key/URL to a saved signature SVG/PNG, if captured. */
        private String signatureUrl;
        /** Typed name of the recipient when no signature was needed. */
        private String recipientName;
        /** GPS at the moment the driver pressed "delivered". */
        private Order.GeoPoint capturedAt;
        private Instant timestamp;
    }

    /** Logistical lifecycle. Runs in parallel to Order.status. */
    public enum DeliveryStatus {
        UNASSIGNED,   // created, not yet on a route
        PLANNED,      // on a route + has a sequence
        DISPATCHED,   // driver started their day, this stop is loaded into the truck
        EN_ROUTE,     // driver is moving toward this stop
        ARRIVED,      // driver pressed "arrived"
        DELIVERED,    // success
        FAILED,       // refused / undeliverable
        CANCELLED     // cancelled before delivery
    }
}