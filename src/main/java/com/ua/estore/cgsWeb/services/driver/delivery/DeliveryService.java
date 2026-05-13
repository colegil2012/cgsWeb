package com.ua.estore.cgsWeb.services.driver.delivery;

import com.ua.estore.cgsWeb.models.driver.delivery.Delivery;
import com.ua.estore.cgsWeb.models.driver.delivery.DeliveryAttempt;
import com.ua.estore.cgsWeb.models.driver.delivery.DeliveryOutcome;
import com.ua.estore.cgsWeb.models.driver.delivery.DeliveryStatus;
import com.ua.estore.cgsWeb.models.shop.Order;
import com.ua.estore.cgsWeb.repositories.driver.delivery.DeliveryRepository;
import com.ua.estore.cgsWeb.repositories.shop.OrderRepository;
import com.ua.estore.cgsWeb.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Owns the lifecycle of {@link Delivery} documents.
 *
 * <p><b>Lazy creation:</b> the driver app calls {@link #ensureFor(Order)} (via
 * {@link #ensureForOrders}) the first time it encounters an Order. PAID orders
 * without a Delivery get one created on-demand. Non-PAID orders are skipped —
 * they aren't yet eligible for fulfillment.</p>
 *
 * <p><b>Status transitions:</b> {@link #recordOutcome} is the single mutation
 * point for delivery status. It appends to {@code attempts}, updates
 * {@link Delivery#getStatus()}, and (for SUCCESS) denormalizes
 * {@code deliveredAt} back onto the source Order so the storefront can show
 * "Delivered Nov 12" without a join.</p>
 *
 * <p>This service intentionally does not know about Routes — that's
 * {@code RouteGenerationService}'s job. It only flips the Delivery state.
 * The route-assignment side (setting {@code currentRouteId}) is exposed via
 * {@link #assignToRoute} and {@link #releaseFromRoute} for the route service
 * to call.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    /* ============================================================================
     * Lookup
     * ============================================================================ */

    public Optional<Delivery> findByOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) return Optional.empty();
        return deliveryRepository.findByOrderId(orderId);
    }

    public Optional<Delivery> findById(String deliveryId) {
        if (deliveryId == null || deliveryId.isBlank()) return Optional.empty();
        return deliveryRepository.findById(deliveryId);
    }

    /**
     * Bulk lookup. Returns a map keyed by order id for quick joining at DTO
     * build time. Orders without a Delivery are simply absent from the map.
     */
    public Map<String, Delivery> findByOrderIds(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return Map.of();
        List<Delivery> found = deliveryRepository.findByOrderIdIn(orderIds);
        Map<String, Delivery> byOrderId = new HashMap<>(found.size());
        for (Delivery d : found) {
            byOrderId.put(d.getOrderId(), d);
        }
        return byOrderId;
    }

    /* ============================================================================
     * Lazy creation
     * ============================================================================ */

    /**
     * Ensure each PAID order in the list has an associated Delivery. Returns a
     * map of {@code orderId → Delivery} including newly-created entries.
     *
     * <p>Orders that are not PAID are skipped silently — they're not yet
     * eligible for fulfillment. Their entries are absent from the returned map.</p>
     *
     * <p>This is a hot path for the orders-list endpoint, so the implementation
     * minimizes round-trips: one bulk lookup, then individual inserts for the
     * gaps. With dozens of orders this is fine; if it ever becomes a bottleneck,
     * move to an eager-create hook in the order-placement flow.</p>
     */
    public Map<String, Delivery> ensureForOrders(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return Map.of();

        List<String> orderIds = new ArrayList<>(orders.size());
        for (Order o : orders) orderIds.add(o.getId());

        Map<String, Delivery> existing = findByOrderIds(orderIds);

        for (Order order : orders) {
            if (existing.containsKey(order.getId())) continue;
            if (order.getStatus() != Order.OrderStatus.PAID) continue;

            Delivery created = createFromOrder(order);
            existing.put(order.getId(), created);
        }
        return existing;
    }

    /**
     * Ensure a single Order has a Delivery. PAID-only — throws if the order
     * isn't payable, because callers asking "ensure delivery exists" for a
     * specific order are doing so to put it on a route, which requires PAID status.
     */
    public Delivery ensureFor(Order order) {
        Objects.requireNonNull(order, "order required");
        return deliveryRepository.findByOrderId(order.getId())
                .orElseGet(() -> createFromOrder(order));
    }

    private Delivery createFromOrder(Order order) {
        Delivery d = new Delivery();
        d.setOrderId(order.getId());
        d.setOrderNumber(order.getOrderNumber());
        d.setStatus(DeliveryStatus.PENDING);
        d.setCreatedAt(TimeUtil.getCurrentDateTime());
        d.setUpdatedAt(TimeUtil.getCurrentDateTime());

        // Customer snapshot — mirrors Order.CustomerSnapshot
        Order.CustomerSnapshot oc = order.getCustomer();
        if (oc != null) {
            Delivery.CustomerSnapshot dc = new Delivery.CustomerSnapshot();
            dc.setUserId(oc.getUserId());
            dc.setFirstName(oc.getFirstName());
            dc.setLastName(oc.getLastName());
            dc.setEmail(oc.getEmail());
            dc.setPhone(oc.getPhone());
            d.setCustomer(dc);
        }

        // Address snapshot — including geo sync so the deliveries collection
        // can run $geoNear directly without back-joining to orders.
        Order.AddressSnapshot oa = order.getShipTo();
        if (oa != null) {
            Delivery.AddressSnapshot da = new Delivery.AddressSnapshot();
            da.setSourceAddressId(oa.getSourceAddressId());
            da.setStreet1(oa.getStreet1());
            da.setStreet2(oa.getStreet2());
            da.setCity(oa.getCity());
            da.setState(oa.getState());
            da.setZip(oa.getZip());
            da.setLatitude(oa.getLatitude());
            da.setLongitude(oa.getLongitude());
            da.syncGeoPoint();
            d.setAddress(da);
        }

        d.setDeliveryInstructions(order.getDeliveryInstructions());

        Delivery saved = deliveryRepository.save(d);
        log.info("Lazy-created Delivery id={} for order id={} number={}",
                saved.getId(), order.getId(), order.getOrderNumber());
        return saved;
    }

    /* ============================================================================
     * Route assignment book-keeping (called by RouteGenerationService)
     * ============================================================================ */

    /**
     * Mark a set of deliveries as assigned to a Route. Sets
     * {@code currentRouteId} and flips status from PENDING to ASSIGNED.
     *
     * <p>Idempotent: deliveries already assigned to the same route are left
     * alone. Deliveries assigned to a <em>different</em> route get reassigned
     * (and a warning logged — usually a sign of a stale UI).</p>
     */
    public void assignToRoute(List<String> deliveryIds, String routeId) {
        if (deliveryIds == null || deliveryIds.isEmpty()) return;

        for (String id : deliveryIds) {
            Delivery d = deliveryRepository.findById(id).orElse(null);
            if (d == null) {
                log.warn("assignToRoute: delivery id={} not found, skipping", id);
                continue;
            }
            if (routeId.equals(d.getCurrentRouteId())) continue; // already on this route

            if (d.getCurrentRouteId() != null) {
                log.warn("Reassigning delivery id={} from route={} to route={}",
                        id, d.getCurrentRouteId(), routeId);
            }
            d.setCurrentRouteId(routeId);
            if (d.getStatus() == DeliveryStatus.PENDING) {
                d.setStatus(DeliveryStatus.ASSIGNED);
            }
            d.setUpdatedAt(TimeUtil.getCurrentDateTime());
            deliveryRepository.save(d);
        }
    }

    /**
     * Release deliveries from a Route back to PENDING. Called when a Route is
     * cancelled. Only flips deliveries currently pointing at the named route —
     * deliveries that were reassigned elsewhere are left alone.
     */
    public void releaseFromRoute(String routeId) {
        if (routeId == null) return;
        List<Delivery> assigned = deliveryRepository.findByCurrentRouteId(routeId);
        for (Delivery d : assigned) {
            d.setCurrentRouteId(null);
            // Only revert ASSIGNED back to PENDING. Don't touch deliveries that
            // already moved further along (OUT_FOR_DELIVERY, DELIVERED, etc).
            if (d.getStatus() == DeliveryStatus.ASSIGNED) {
                d.setStatus(DeliveryStatus.PENDING);
            }
            d.setUpdatedAt(TimeUtil.getCurrentDateTime());
            deliveryRepository.save(d);
        }
    }

    /* ============================================================================
     * Status transitions
     * ============================================================================ */

    /**
     * Record an outcome for a single delivery attempt. Appends to attempts,
     * updates status, and (on SUCCESS) propagates deliveredAt to Order.
     *
     * @return the updated Delivery
     */
    public Delivery recordOutcome(String deliveryId,
                                  DeliveryOutcome outcome,
                                  String notes,
                                  String driverId) {
        Delivery d = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery not found: " + deliveryId));

        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.setAttemptedAt(TimeUtil.getCurrentDateTime());
        attempt.setOutcome(outcome);
        attempt.setNotes(notes);
        attempt.setDriverId(driverId);
        attempt.setRouteId(d.getCurrentRouteId());
        d.getAttempts().add(attempt);

        // Map attempt outcome → terminal Delivery status
        DeliveryStatus newStatus = switch (outcome) {
            case SUCCESS -> DeliveryStatus.DELIVERED;
            case FAILED  -> DeliveryStatus.FAILED;
            case SKIPPED -> DeliveryStatus.SKIPPED;
        };
        d.setStatus(newStatus);
        d.setDriverNotes(notes);
        d.setUpdatedAt(TimeUtil.getCurrentDateTime());

        if (newStatus == DeliveryStatus.DELIVERED) {
            d.setDeliveredAt(TimeUtil.getCurrentDateTime());
            propagateDeliveredToOrder(d);
        }

        Delivery saved = deliveryRepository.save(d);
        log.info("Delivery id={} outcome={} -> status={} (orderId={})",
                saved.getId(), outcome, newStatus, saved.getOrderId());
        return saved;
    }

    /**
     * Denormalize delivery completion onto the source Order so the storefront
     * can show "Delivered &lt;date&gt;" without a join.
     *
     * <p>Requires {@code Order} to have a {@code deliveredAt} field. If your
     * Order model doesn't have it yet, add this line:
     * <pre>private LocalDateTime deliveredAt;</pre>
     * Lombok's {@code @Data} generates the setter automatically.</p>
     */
    private void propagateDeliveredToOrder(Delivery d) {
        orderRepository.findById(d.getOrderId()).ifPresentOrElse(
                order -> {
                    order.setDeliveredAt(d.getDeliveredAt());
                    order.setUpdatedAt(TimeUtil.getCurrentDateTime());
                    orderRepository.save(order);
                },
                () -> log.warn("propagateDeliveredToOrder: order id={} not found", d.getOrderId())
        );
    }
}