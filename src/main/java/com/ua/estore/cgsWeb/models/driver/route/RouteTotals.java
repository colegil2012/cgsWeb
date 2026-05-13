package com.ua.estore.cgsWeb.models.driver.route;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregate route metrics. Denormalized so the kiosk can show "12 stops,
 * 38 mi, ~2h 15m" without re-summing individual stop legs each render.
 */
@Data
@NoArgsConstructor
public class RouteTotals {

    /** Sum of all {@code RouteStop.legDistanceMeters} (plus return-to-origin leg if included). */
    private Long distanceMeters;

    /** Sum of all {@code RouteStop.legDurationSeconds}. Does not include time spent at stops. */
    private Long durationSeconds;

    /** Convenience copy of {@code Route.stops.size()}. */
    private Integer stopCount;
}