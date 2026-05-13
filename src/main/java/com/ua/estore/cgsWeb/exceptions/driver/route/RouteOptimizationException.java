package com.ua.estore.cgsWeb.exceptions.driver.route;

/**
 * Thrown by {@link RouteOptimizer} implementations when optimization fails.
 * Distinct from {@code DriverApiException} because optimizers don't know about
 * the HTTP layer — they speak the language of waypoints and routes. The
 * service layer catches this and translates it.
 */
public class RouteOptimizationException extends RuntimeException {

    public RouteOptimizationException(String message) {
        super(message);
    }

    public RouteOptimizationException(String message, Throwable cause) {
        super(message, cause);
    }
}