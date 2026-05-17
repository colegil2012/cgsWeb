package com.ua.estore.cgsWeb.exceptions.driver.route;

import lombok.Getter;

/**
 * Thrown when a route generation request is rejected because the driver
 * already has a route in {@code PLANNED} or {@code IN_PROGRESS} status.
 *
 * <p>Maps to HTTP 409 Conflict in {@code DriverApiExceptionHandler}. The
 * existing route's id is included in the error response so the kiosk can
 * navigate the driver to it instead of leaving them stuck.</p>
 *
 * <p>This guard fires <em>after</em> the idempotency check — a repeated POST
 * with the same key returns the existing route (200), not a 409, so slow
 * networks and retries don't accidentally trip the active-route block.</p>
 */
@Getter
public class RouteAlreadyActiveException extends RuntimeException {

    private final String activeRouteId;

    public RouteAlreadyActiveException(String activeRouteId) {
        super("A route is already active (id=" + activeRouteId
                + "). Complete or cancel it before planning a new one.");
        this.activeRouteId = activeRouteId;
    }
}