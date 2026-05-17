package com.ua.estore.cgsWeb.services.admin;

import com.ua.estore.cgsWeb.models.dto.admin.AdminRouteListItemDTO;
import com.ua.estore.cgsWeb.models.dto.admin.AdminRouteStopView;
import com.ua.estore.cgsWeb.models.driver.delivery.Delivery;
import com.ua.estore.cgsWeb.models.driver.route.Route;
import com.ua.estore.cgsWeb.models.driver.route.RouteStatus;
import com.ua.estore.cgsWeb.models.driver.route.RouteStop;
import com.ua.estore.cgsWeb.models.shop.Order;
import com.ua.estore.cgsWeb.repositories.driver.delivery.DeliveryRepository;
import com.ua.estore.cgsWeb.repositories.driver.route.RouteRepository;
import com.ua.estore.cgsWeb.repositories.shop.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Admin-side route browsing. Read-only.
 *
 * <p>The detail view performs the {@code Route → Delivery → Order} join. Both
 * hops are batch lookups (one query each, not per-stop) so a route with 20
 * stops costs 3 queries total: the route, all its deliveries, all their
 * orders.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRouteService {

    private static final int PAGE_SIZE = 50;

    private final RouteRepository routeRepository;
    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    /* ============================================================================
     * List
     * ============================================================================ */

    public PagedRouteResult list(String statusFilter, int page) {
        RouteStatus status = parseStatus(statusFilter);
        Pageable pageable = PageRequest.of(Math.max(0, page), PAGE_SIZE);

        Page<Route> result = (status == null)
                ? routeRepository.findAllByOrderByCreatedAtDesc(pageable)
                : routeRepository.findByStatusOrderByCreatedAtDesc(status, pageable);

        List<AdminRouteListItemDTO> items = result.getContent().stream()
                .map(AdminRouteListItemDTO::from)
                .toList();

        return new PagedRouteResult(
                items, result.getNumber(), result.getTotalPages(),
                result.getTotalElements(),
                status == null ? null : status.name());
    }

    /* ============================================================================
     * Detail
     * ============================================================================ */

    /**
     * Load a route and resolve each stop through to its delivery and order.
     *
     * <p>Three queries total regardless of stop count:</p>
     * <ol>
     *   <li>the route itself</li>
     *   <li>all deliveries for the route's stop deliveryIds (batch)</li>
     *   <li>all orders for those deliveries' orderIds (batch)</li>
     * </ol>
     */
    public Optional<RouteDetail> findRouteDetail(String routeId) {
        Optional<Route> routeOpt = routeRepository.findById(routeId);
        if (routeOpt.isEmpty()) return Optional.empty();
        Route route = routeOpt.get();

        List<RouteStop> stops = route.getStops() == null ? List.of() : route.getStops();

        // ---- Batch 1: deliveries ----
        List<String> deliveryIds = stops.stream()
                .map(RouteStop::getDeliveryId)
                .filter(id -> id != null && !id.isBlank())
                .toList();

        Map<String, Delivery> deliveriesById = new HashMap<>();
        if (!deliveryIds.isEmpty()) {
            for (Delivery d : deliveryRepository.findAllById(deliveryIds)) {
                deliveriesById.put(d.getId(), d);
            }
        }

        // ---- Batch 2: orders (via the deliveries' orderIds) ----
        List<String> orderIds = deliveriesById.values().stream()
                .map(Delivery::getOrderId)
                .filter(id -> id != null && !id.isBlank())
                .toList();

        Map<String, Order> ordersById = new HashMap<>();
        if (!orderIds.isEmpty()) {
            for (Order o : orderRepository.findAllById(orderIds)) {
                ordersById.put(o.getId(), o);
            }
        }

        // ---- Assemble per-stop views ----
        List<AdminRouteStopView> stopViews = new ArrayList<>();
        for (RouteStop stop : stops) {
            Delivery delivery = stop.getDeliveryId() == null
                    ? null : deliveriesById.get(stop.getDeliveryId());
            Order order = (delivery != null && delivery.getOrderId() != null)
                    ? ordersById.get(delivery.getOrderId())
                    : null;
            stopViews.add(AdminRouteStopView.of(stop, delivery, order));
        }
        // Stops should already be in sequence order (Mongo preserves array
        // order), but sort defensively.
        stopViews.sort((a, b) -> Integer.compare(a.getSequence(), b.getSequence()));

        return Optional.of(new RouteDetail(route, stopViews));
    }

    /* ============================================================================
     * Helpers
     * ============================================================================ */

    static RouteStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return RouteStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /* ============================================================================
     * Result types
     * ============================================================================ */

    public record PagedRouteResult(
            List<AdminRouteListItemDTO> items,
            int page,
            int totalPages,
            long totalElements,
            String statusFilter
    ) {}

    public record RouteDetail(
            Route route,
            List<AdminRouteStopView> stops
    ) {}
}