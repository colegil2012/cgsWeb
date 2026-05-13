package com.ua.estore.cgsWeb.models.driver.delivery;

/**
 * The logistical state machine for a Delivery.
 *
 * <p>Valid transitions:</p>
 * <pre>
 *   PENDING ──► ASSIGNED ──► OUT_FOR_DELIVERY ──► DELIVERED
 *      │           │                │                 ▲
 *      │           │                ├──► FAILED ──────┤  (after retry)
 *      │           │                └──► SKIPPED      │
 *      │           └──► (route cancelled) ──► PENDING │
 *      └──► SKIPPED (customer requested cancellation)
 * </pre>
 *
 * <p>Transitions are enforced by {@code DeliveryService}, not the model. The
 * enum just names the states.</p>
 */
public enum DeliveryStatus {

    /** Delivery exists but isn't on any route yet. The default after creation. */
    PENDING,

    /** Placed on a Route, but the driver hasn't started the route yet. */
    ASSIGNED,

    /** Route is IN_PROGRESS and the driver is on the way to this stop. */
    OUT_FOR_DELIVERY,

    /** Successfully delivered. Terminal in the happy path. */
    DELIVERED,

    /** Driver attempted but couldn't complete (nobody home, address issue, etc). May be retried later. */
    FAILED,

    /** Intentionally not delivered (customer cancellation, driver decision). Terminal. */
    SKIPPED
}