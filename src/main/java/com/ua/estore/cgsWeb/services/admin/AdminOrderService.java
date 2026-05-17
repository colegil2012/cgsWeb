package com.ua.estore.cgsWeb.services.admin;

import com.ua.estore.cgsWeb.models.dto.admin.AdminOrderListItemDTO;
import com.ua.estore.cgsWeb.models.driver.delivery.Delivery;
import com.ua.estore.cgsWeb.models.driver.route.Route;
import com.ua.estore.cgsWeb.models.shop.Order;
import com.ua.estore.cgsWeb.repositories.driver.delivery.DeliveryRepository;
import com.ua.estore.cgsWeb.repositories.driver.route.RouteRepository;
import com.ua.estore.cgsWeb.repositories.shop.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Admin-side order browsing. Read-only — orders are financial records and the
 * admin views never mutate them.
 *
 * <p>The detail view resolves the order's delivery and, through it, the route
 * the order is (or was) on — see {@link #findOrderDetail}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private static final int PAGE_SIZE = 50;

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final RouteRepository routeRepository;

    /* ============================================================================
     * List
     * ============================================================================ */

    /**
     * Paged order list with an optional status filter and optional search term.
     *
     * @param statusFilter null/blank/unknown = no status filter
     * @param search       null/blank = no search; else matches order number or
     *                     customer name (regex-escaped here)
     */
    public PagedOrderResult list(String statusFilter, String search, int page) {
        Order.OrderStatus status = parseStatus(statusFilter);
        String escaped = (search == null || search.trim().isEmpty())
                ? null : escapeRegex(search.trim());

        Pageable pageable = PageRequest.of(
                Math.max(0, page), PAGE_SIZE, Sort.by("placedAt").descending());

        Page<Order> result;
        if (escaped != null && status != null) {
            result = orderRepository.searchByNumberOrCustomerAndStatus(escaped, status, pageable);
        } else if (escaped != null) {
            result = orderRepository.searchByNumberOrCustomer(escaped, pageable);
        } else if (status != null) {
            result = orderRepository.findByStatus(status, pageable);
        } else {
            result = orderRepository.findAll(pageable);
        }

        List<AdminOrderListItemDTO> items = result.getContent().stream()
                .map(AdminOrderListItemDTO::from)
                .toList();

        return new PagedOrderResult(
                items, result.getNumber(), result.getTotalPages(),
                result.getTotalElements(),
                status == null ? null : status.name(),
                search == null ? null : search.trim());
    }

    /* ============================================================================
     * Detail
     * ============================================================================ */

    /**
     * Load an order plus its route linkage for the detail page.
     *
     * <p>Linkage chain: order → {@code deliveryRepository.findByOrderId} →
     * delivery's {@code currentRouteId} → route.</p>
     *
     * <p>{@code currentRouteId} is only populated while the delivery is on an
     * active (PLANNED / IN_PROGRESS) route — a completed or cancelled route
     * clears it. So for a delivered order whose route already completed, the
     * "current" route link is gone. We fall back to the most recent
     * {@code DeliveryAttempt.routeId} so the detail page can still show
     * "delivered on route R-..." historically.</p>
     */
    public Optional<OrderDetail> findOrderDetail(String orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return Optional.empty();
        Order order = orderOpt.get();

        Delivery delivery = deliveryRepository.findByOrderId(orderId).orElse(null);

        Route route = null;
        if (delivery != null) {
            String routeId = delivery.getCurrentRouteId();

            // Fallback: if not currently on a route, use the most recent
            // attempt's routeId (historical linkage).
            if (routeId == null && delivery.getAttempts() != null
                    && !delivery.getAttempts().isEmpty()) {
                for (int i = delivery.getAttempts().size() - 1; i >= 0; i--) {
                    String attemptRouteId = delivery.getAttempts().get(i).getRouteId();
                    if (attemptRouteId != null) {
                        routeId = attemptRouteId;
                        break;
                    }
                }
            }

            if (routeId != null) {
                route = routeRepository.findById(routeId).orElse(null);
            }
        }

        return Optional.of(new OrderDetail(order, delivery, route));
    }

    /* ============================================================================
     * Helpers
     * ============================================================================ */

    static Order.OrderStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Order.OrderStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;   // unknown value — treat as no filter
        }
    }

    static String escapeRegex(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\\\.\\[\\]{}()*+?^$|]", "\\\\$0");
    }

    /* ============================================================================
     * Result types
     * ============================================================================ */

    public record PagedOrderResult(
            List<AdminOrderListItemDTO> items,
            int page,
            int totalPages,
            long totalElements,
            String statusFilter,    // normalized enum name, or null
            String search           // trimmed search term, or null
    ) {}

    /**
     * Order detail bundle. {@code delivery} and {@code route} are nullable —
     * an order may have no delivery yet (lazy creation) and a delivery may
     * not be linked to any route.
     */
    public record OrderDetail(
            Order order,
            Delivery delivery,
            Route route
    ) {}
}