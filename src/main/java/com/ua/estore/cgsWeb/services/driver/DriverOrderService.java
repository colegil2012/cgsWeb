package com.ua.estore.cgsWeb.services.driver;

import com.ua.estore.cgsWeb.models.dto.driver.DriverOrderDTO;
import com.ua.estore.cgsWeb.models.shop.Order;
import com.ua.estore.cgsWeb.repositories.shop.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Driver-facing read service. Bridges the storefront-owned {@link Order} model
 * to the kiosk's {@link DriverOrderDTO} view.
 *
 * <p>Currently returns every order in the collection, newest first. When the
 * {@code Delivery} document lands, this service will:
 *   <ul>
 *     <li>Filter out orders that already have a {@code DELIVERED} delivery.</li>
 *     <li>Lazy-create {@code Delivery(status=PENDING)} for orders without one.</li>
 *     <li>Carry {@code deliveryId} + {@code deliveryStatus} into the DTO.</li>
 *   </ul>
 * Until then, this is a thin pass-through.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverOrderService {

    private final OrderRepository orderRepository;

    /**
     * Returns every order in the collection as a {@link DriverOrderDTO}, sorted
     * by {@code placedAt} descending. No status filter — orders in any state
     * (PENDING, PAID, CANCELLED, REFUNDED) are returned. Tighten this once the
     * Delivery model is in place.
     */
    public List<DriverOrderDTO> listAllOrders() {
        // Sort.Direction.DESC so the most recent orders appear at the top of the
        // kiosk's grid. nullsLast() guards against legacy/test orders that may
        // not have placedAt populated; without it those would crash the sort.
        List<Order> orders = orderRepository.findAll(
                Sort.by(Sort.Order.desc("placedAt").nullsLast())
        );

        return orders.stream()
                .map(DriverOrderDTO::from)
                .toList();
    }

    /**
     * Returns every paid order in the collection as a {@link DriverOrderDTO}, sorted
     * by {@code placedAt} descending, within the specified time range.
     */

    public List<DriverOrderDTO> listPaidOrdersByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<Order> orders = orderRepository.findByStatusAndPlacedAtBetween(
                Order.OrderStatus.PAID, startTime, endTime
        );

        return orders.stream()
                .map(DriverOrderDTO::from)
                .toList();
    }
}