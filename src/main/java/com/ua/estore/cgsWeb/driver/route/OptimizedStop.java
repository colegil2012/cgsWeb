package com.ua.estore.cgsWeb.driver.route;

/**
 * One stop in the optimizer's output, in the chosen order. Leg metrics are
 * relative to the previous point in the route (origin for sequence 1, prior
 * stop otherwise).
 */
public record OptimizedStop(
        String deliveryId,
        int sequence,
        long legDistanceMeters,
        long legDurationSeconds
) {}