package com.ua.estore.cgsWeb.models.driver.route;

/**
 * Lifecycle state of a Route.
 *
 * <p>Valid transitions:</p>
 * <pre>
 *   PLANNED ──► IN_PROGRESS ──► COMPLETED
 *      │              │
 *      └──────────────┴──► CANCELLED
 * </pre>
 *
 * <p>Note: a Route can be CANCELLED from either PLANNED or IN_PROGRESS.
 * Cancelling clears {@code Delivery.currentRouteId} on all referenced
 * Deliveries so they're available for inclusion in a new route.</p>
 */
public enum RouteStatus {

    /** Created but not yet started. Driver can still regenerate / modify. */
    PLANNED,

    /** Driver has started driving the route. Stops are being completed in order. */
    IN_PROGRESS,

    /** All stops resolved (delivered / failed / skipped) and driver marked the route done. */
    COMPLETED,

    /** Abandoned before completion. Referenced Deliveries are released back to PENDING. */
    CANCELLED
}