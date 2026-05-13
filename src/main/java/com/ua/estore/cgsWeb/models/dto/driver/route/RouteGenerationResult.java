package com.ua.estore.cgsWeb.models.dto.driver.route;


import com.ua.estore.cgsWeb.models.driver.route.Route;

/**
 * Result of a route-generation attempt. {@code wasCreated} is {@code true}
 * when a new Route was produced; {@code false} when an existing Route was
 * returned via idempotency-key match.
 *
 * <p>The controller layer uses this to choose between {@code 201 Created} and
 * {@code 200 OK} responses. Internal type — not exposed to the kiosk; the
 * controller projects the Route into a {@link RouteDTO} for the response.</p>
 */
public record RouteGenerationResult(Route route, boolean wasCreated) {}