package com.ua.estore.cgsWeb.driver.route;

import com.ua.estore.cgsWeb.exceptions.driver.route.RouteOptimizationException;

import java.util.List;

/**
 * Abstraction over a route-optimization provider. Implementations:
 * <ul>
 *   <li>Order waypoints into an efficient sequence.</li>
 *   <li>Produce a driving polyline (the "geometry") connecting them.</li>
 *   <li>Report total/leg distances and durations.</li>
 * </ul>
 *
 * <p>Implementations are pure — they don't know about Orders, Deliveries, or
 * MongoDB. They take primitive route data in and return primitive route data
 * out. The service layer translates between this and the domain.</p>
 *
 * <p>Selected at runtime via {@code @ConditionalOnProperty} on
 * {@code celtech.route.optimizer}. Currently the only implementation is
 * {@link OrsRouteOptimizer}; future implementations register under different
 * names.</p>
 */
public interface RouteOptimizer {

    /**
     * Compute the optimal stop order and driving route.
     *
     * @param origin         starting point
     * @param waypoints      stops to visit, in any order
     * @param returnToOrigin whether the route should include a final leg back to origin
     * @return optimized stop order + geometry + totals
     * @throws RouteOptimizationException if optimization fails
     */
    OptimizedRoute optimize(RoutePoint origin, List<RouteWaypoint> waypoints, boolean returnToOrigin);

    /** Identifier for this optimizer, written to {@code OptimizationMetadata.optimizerName}. */
    String getName();

    /**
     * Maximum waypoint count this optimizer can handle. The service layer uses
     * this to preflight requests and throw a clean exception before the
     * optimizer would have errored out internally.
     */
    int getMaxWaypoints();
}