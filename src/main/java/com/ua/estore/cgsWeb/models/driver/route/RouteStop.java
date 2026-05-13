package com.ua.estore.cgsWeb.models.driver.route;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

/**
 * A single stop on a Route. Deliberately minimal — no snapshotted customer or
 * address fields, no status field. Per-stop info is read from the referenced
 * Delivery at DTO build time.
 *
 * <p>This means the Route is not durable to direct edits of the referenced
 * Delivery (if Delivery.address ever mutated, the route's displayed address
 * would change too). The chain is safe today because Deliveries themselves
 * snapshot from immutable Orders, but if Delivery ever gains mutable fields
 * affecting display, revisit whether RouteStop should snapshot.</p>
 *
 * <p><b>Leg metrics</b> ({@link #legDistanceMeters}, {@link #legDurationSeconds})
 * are the driving distance/time from the <em>previous</em> point in the route
 * (either the origin for stop 1, or the previous stop for stops 2..N) to this
 * stop. Used by the kiosk to display "12 min to next stop" hints.</p>
 */
@Data
@NoArgsConstructor
public class RouteStop {

    /** 1-indexed position in the route. Matches the index+1 of this stop in Route.stops. */
    private Integer sequence;

    /**
     * Pointer to the Delivery being made at this stop. The Delivery is the
     * source of truth for customer info, address, instructions, and current
     * status. To render this stop, load the Delivery.
     */
    @Field(targetType = FieldType.OBJECT_ID)
    private String deliveryId;

    /**
     * Estimated driving distance from the previous point to this stop, in meters.
     * "Previous point" is the route origin for {@code sequence == 1}, or the
     * preceding stop for {@code sequence > 1}.
     */
    private Long legDistanceMeters;

    /** Estimated driving time from the previous point to this stop, in seconds. */
    private Long legDurationSeconds;
}