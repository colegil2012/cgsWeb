package com.ua.estore.cgsWeb.services.driver.route;

import com.ua.estore.cgsWeb.config.props.RouteProperties;
import com.ua.estore.cgsWeb.driver.route.*;
import com.ua.estore.cgsWeb.exceptions.driver.OrderNotFoundException;
import com.ua.estore.cgsWeb.exceptions.driver.OrderNotPayableException;
import com.ua.estore.cgsWeb.exceptions.driver.RouteCapacityExceededException;
import com.ua.estore.cgsWeb.models.driver.delivery.Delivery;
import com.ua.estore.cgsWeb.models.driver.route.*;
import com.ua.estore.cgsWeb.models.dto.driver.route.GenerateRouteRequest;
import com.ua.estore.cgsWeb.models.dto.driver.route.RouteGenerationResult;
import com.ua.estore.cgsWeb.models.shop.Order;
import com.ua.estore.cgsWeb.repositories.driver.route.RouteRepository;
import com.ua.estore.cgsWeb.repositories.shop.OrderRepository;
import com.ua.estore.cgsWeb.services.driver.delivery.DeliveryService;
import com.ua.estore.cgsWeb.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrates route generation. Takes a list of selected Order ids and turns
 * them into a persisted {@link Route} document with optimized stop order and
 * driving geometry.
 *
 * <p>Returns a {@link RouteGenerationResult} so callers can distinguish "I
 * created a new Route" from "I matched an existing one via idempotency key" —
 * the controller layer translates that into 201 vs 200 responses.</p>
 *
 * <p>See class Javadoc on prior versions for the full eventual-consistency
 * and idempotency story.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteGenerationService {

    private final OrderRepository orderRepository;
    private final RouteRepository routeRepository;
    private final DeliveryService deliveryService;
    private final RouteOptimizer optimizer;
    private final RouteProperties routeProperties;

    public RouteGenerationResult generate(GenerateRouteRequest request) {
        validateRequest(request);

        // Step 1: idempotency check.
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Route existing = routeRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElse(null);
            if (existing != null) {
                log.info("Returning existing route id={} for idempotencyKey={}",
                        existing.getId(), request.getIdempotencyKey());
                return new RouteGenerationResult(existing, false);
            }
        }

        // Step 2: load + validate orders
        List<Order> orders = loadAndValidateOrders(request.getOrderIds());

        // Step 3: preflight capacity
        if (orders.size() > optimizer.getMaxWaypoints()) {
            throw new RouteCapacityExceededException(orders.size(), optimizer.getMaxWaypoints());
        }

        // Step 4: ensure Deliveries exist
        Map<String, Delivery> deliveriesByOrderId = deliveryService.ensureForOrders(orders);

        // Step 5: build waypoints from Delivery snapshots
        List<RouteWaypoint> waypoints = buildWaypoints(orders, deliveriesByOrderId);

        // Step 6: optimize
        RoutePoint origin = resolveOrigin(request);
        long optStart = System.currentTimeMillis();
        OptimizedRoute optimized = optimizer.optimize(origin, waypoints, true);
        log.info("Optimizer {} produced {} stops in {}ms (total {} m, {} s)",
                optimizer.getName(),
                optimized.stops().size(),
                System.currentTimeMillis() - optStart,
                optimized.totalDistanceMeters(),
                optimized.totalDurationSeconds());

        // Step 7: assemble + persist
        Route route = assembleRoute(request, origin, optimized);
        Route saved = routeRepository.save(route);

        // Step 8: assign deliveries
        try {
            List<String> deliveryIds = saved.getStops().stream()
                    .map(RouteStop::getDeliveryId)
                    .toList();
            deliveryService.assignToRoute(deliveryIds, saved.getId());
        } catch (RuntimeException e) {
            log.error("Failed to assign deliveries to route id={}; route persisted but unassigned. " +
                    "Deliveries will appear PENDING on next list call.", saved.getId(), e);
        }

        return new RouteGenerationResult(saved, true);
    }

    /* ============================================================================
     * Steps
     * ============================================================================ */

    private void validateRequest(GenerateRouteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body required");
        }
        if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
            throw new IllegalArgumentException("At least one orderId required");
        }
    }

    private List<Order> loadAndValidateOrders(List<String> orderIds) {
        Set<String> seen = new HashSet<>();
        List<String> dedupedIds = new ArrayList<>(orderIds.size());
        for (String id : orderIds) {
            if (id != null && !id.isBlank() && seen.add(id)) dedupedIds.add(id);
        }

        List<Order> found = orderRepository.findAllById(dedupedIds);
        Map<String, Order> byId = new HashMap<>(found.size());
        for (Order o : found) byId.put(o.getId(), o);

        List<String> missing = new ArrayList<>();
        for (String id : dedupedIds) {
            if (!byId.containsKey(id)) missing.add(id);
        }
        if (!missing.isEmpty()) throw new OrderNotFoundException(missing);

        Map<String, String> notPayable = new LinkedHashMap<>();
        for (String id : dedupedIds) {
            Order o = byId.get(id);
            if (o.getStatus() != Order.OrderStatus.PAID) {
                notPayable.put(id, o.getStatus() != null ? o.getStatus().name() : "null");
            }
        }
        if (!notPayable.isEmpty()) throw new OrderNotPayableException(notPayable);

        List<Order> ordered = new ArrayList<>(dedupedIds.size());
        for (String id : dedupedIds) ordered.add(byId.get(id));
        return ordered;
    }

    private List<RouteWaypoint> buildWaypoints(List<Order> orders, Map<String, Delivery> deliveriesByOrderId) {
        List<RouteWaypoint> waypoints = new ArrayList<>(orders.size());
        for (Order order : orders) {
            Delivery d = deliveriesByOrderId.get(order.getId());
            if (d == null) {
                throw new IllegalStateException("Missing Delivery for order " + order.getId());
            }
            Delivery.AddressSnapshot addr = d.getAddress();
            if (addr == null) {
                throw new IllegalStateException("Delivery " + d.getId() + " has no address snapshot");
            }
            waypoints.add(new RouteWaypoint(d.getId(), addr.getLatitude(), addr.getLongitude()));
        }
        return waypoints;
    }

    private RoutePoint resolveOrigin(GenerateRouteRequest request) {
        if (request.getOrigin() != null
                && request.getOrigin().getLatitude() != null
                && request.getOrigin().getLongitude() != null) {
            return new RoutePoint(
                    request.getOrigin().getLatitude(),
                    request.getOrigin().getLongitude());
        }
        RouteProperties.HomeBase hb = routeProperties.homeBase();
        if (hb == null) {
            throw new IllegalStateException(
                    "No origin override supplied and celtech.route.home-base is not configured");
        }
        return new RoutePoint(hb.latitude(), hb.longitude());
    }

    private Route assembleRoute(GenerateRouteRequest request, RoutePoint origin, OptimizedRoute optimized) {
        Route route = new Route();
        route.setRouteNumber(generateRouteNumber());
        route.setStatus(RouteStatus.PLANNED);
        route.setIdempotencyKey(request.getIdempotencyKey());
        route.setCreatedAt(TimeUtil.getCurrentDateTime());
        route.setUpdatedAt(TimeUtil.getCurrentDateTime());

        Route.RoutePoint o = new Route.RoutePoint(origin.latitude(), origin.longitude());
        route.setOrigin(o);

        List<RouteStop> stops = new ArrayList<>(optimized.stops().size());
        for (OptimizedStop os : optimized.stops()) {
            RouteStop stop = new RouteStop();
            stop.setSequence(os.sequence());
            stop.setDeliveryId(os.deliveryId());
            stop.setLegDistanceMeters(os.legDistanceMeters());
            stop.setLegDurationSeconds(os.legDurationSeconds());
            stops.add(stop);
        }
        route.setStops(stops);

        route.setGeometry(new GeoJsonLineString(optimized.geometryCoordinates()));

        RouteTotals totals = new RouteTotals();
        totals.setDistanceMeters(optimized.totalDistanceMeters());
        totals.setDurationSeconds(optimized.totalDurationSeconds());
        totals.setStopCount(stops.size());
        route.setTotals(totals);

        OptimizationMetadata meta = new OptimizationMetadata();
        meta.setOptimizerName(optimizer.getName());
        meta.setGeometryProvider(optimized.geometryProvider());
        meta.setOptimizerVersion(optimized.optimizerVersion());
        meta.setOptimizedAt(LocalDateTime.now());
        meta.setElapsedMillis(optimized.elapsedMillis());
        route.setOptimization(meta);

        return route;
    }

    private static String generateRouteNumber() {
        return "R-" + Year.now() + "-" + (System.currentTimeMillis() % 100_000_000L);
    }
}