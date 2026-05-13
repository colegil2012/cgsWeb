package com.ua.estore.cgsWeb.driver.route;

/**
 * A stop on a route, as seen by the optimizer. Carries the Delivery id so the
 * optimizer can return ordered Delivery ids back to the service layer without
 * needing an external mapping table.
 *
 * <p>Optimizers that require integer ids (VROOM does) should map the string
 * deliveryId to a local integer index internally, then translate back when
 * building the OptimizedRoute response.</p>
 */
public record RouteWaypoint(String deliveryId, double latitude, double longitude) {}