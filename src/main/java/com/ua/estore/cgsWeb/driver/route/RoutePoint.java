package com.ua.estore.cgsWeb.driver.route;

/**
 * A bare lat/lng pair. Used as the route's origin (the optimizer doesn't need
 * to know what's at the origin — just where it is). Mirrors the shape of
 * {@code Route.RoutePoint} but lives in the routing package so optimizers don't
 * depend on domain models.
 */
public record RoutePoint(double latitude, double longitude) {}