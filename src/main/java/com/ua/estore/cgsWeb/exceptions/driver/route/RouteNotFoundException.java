package com.ua.estore.cgsWeb.exceptions.driver.route;

import lombok.Getter;

/**
 * Thrown when a route id doesn't resolve to a persisted Route.
 *
 * <p>Used by the lifecycle service (start/complete/cancel) — the read-side
 * GET endpoint reports missing routes with a plain 404 status (no body),
 * so this exception isn't needed there.</p>
 *
 * <p>Maps to HTTP 404 in {@code DriverApiExceptionHandler}.</p>
 */
@Getter
public class RouteNotFoundException extends RuntimeException {

    private final String routeId;

    public RouteNotFoundException(String routeId) {
        super("Route not found: " + routeId);
        this.routeId = routeId;
    }
}