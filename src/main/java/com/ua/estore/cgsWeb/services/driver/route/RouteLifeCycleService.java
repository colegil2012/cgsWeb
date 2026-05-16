package com.ua.estore.cgsWeb.services.driver.route;

import com.ua.estore.cgsWeb.exceptions.driver.route.InvalidRouteTransitionException;
import com.ua.estore.cgsWeb.exceptions.driver.route.RouteNotFoundException;
import com.ua.estore.cgsWeb.models.driver.route.Route;
import com.ua.estore.cgsWeb.models.driver.route.RouteStatus;
import com.ua.estore.cgsWeb.repositories.driver.route.RouteRepository;
import com.ua.estore.cgsWeb.services.driver.delivery.DeliveryService;
import com.ua.estore.cgsWeb.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteLifeCycleService {

    /** Statuses that count as "active." Mirror this in {@link RouteGenerationService}. */
    private static final List<RouteStatus> ACTIVE_STATUSES =
            List.of(RouteStatus.PLANNED, RouteStatus.IN_PROGRESS);

    private final RouteRepository routeRepository;
    private final DeliveryService deliveryService;

    /* ============================================================================
     * Read: find the active route
     * ============================================================================ */

    /**
     * Returns the currently-active route, if any. "Active" means
     * {@code status ∈ {PLANNED, IN_PROGRESS}}. Empty when no such route
     * exists — the caller is expected to map that to an HTTP 404.
     *
     * <p>Under the active-route invariant enforced by
     * {@link RouteGenerationService}, there should be at most one such route.
     * If somehow more than one exists, the repository's
     * {@code findFirstByStatusInOrderByCreatedAtDesc} returns the most recent
     * one, making this read deterministic.</p>
     */
    public Optional<Route> findActive() {
        return routeRepository.findFirstByStatusInOrderByCreatedAtDesc(ACTIVE_STATUSES);
    }

    /* ============================================================================
     * Write: state transitions
     * ============================================================================ */

    /**
     * Transition PLANNED → IN_PROGRESS. Sets {@code startedAt}.
     *
     * @throws RouteNotFoundException if the id doesn't resolve
     * @throws InvalidRouteTransitionException if the route isn't PLANNED
     */
    public Route start(String routeId) {
        Route route = loadRouteOrThrow(routeId);

        if (route.getStatus() != RouteStatus.PLANNED) {
            throw new InvalidRouteTransitionException(
                    route.getStatus(), "start", List.of(RouteStatus.PLANNED));
        }

        route.setStatus(RouteStatus.IN_PROGRESS);
        route.setStartedAt(TimeUtil.getCurrentDateTime());
        route.setUpdatedAt(TimeUtil.getCurrentDateTime());
        Route saved = routeRepository.save(route);
        log.info("Route id={} started (was PLANNED)", saved.getId());
        return saved;
    }

    /**
     * Transition IN_PROGRESS → COMPLETED. Sets {@code completedAt}.
     *
     * <p>This is an <em>explicit</em> action — the route is never
     * auto-completed when the last delivery is marked. The driver always taps
     * Complete deliberately, which is the design choice that makes finishing
     * a route a real moment rather than a side-effect.</p>
     *
     * <p>Note: deliveries stay where they are. A delivered route's
     * deliveries are already in their terminal states (DELIVERED, FAILED, or
     * SKIPPED). The {@code currentRouteId} pointer remains for historical
     * reference — querying "what route did this delivery come from?" is more
     * useful than wiping the link.</p>
     *
     * @throws RouteNotFoundException if the id doesn't resolve
     * @throws InvalidRouteTransitionException if the route isn't IN_PROGRESS
     */
    public Route complete(String routeId) {
        Route route = loadRouteOrThrow(routeId);

        if (route.getStatus() != RouteStatus.IN_PROGRESS) {
            throw new InvalidRouteTransitionException(
                    route.getStatus(), "complete", List.of(RouteStatus.IN_PROGRESS));
        }

        route.setStatus(RouteStatus.COMPLETED);
        route.setCompletedAt(TimeUtil.getCurrentDateTime());
        route.setUpdatedAt(TimeUtil.getCurrentDateTime());
        Route saved = routeRepository.save(route);
        log.info("Route id={} completed (was IN_PROGRESS)", saved.getId());
        return saved;
    }

    /**
     * Cancel an active route. Allowed from either PLANNED or IN_PROGRESS.
     * Sets status → CANCELLED and releases all attached deliveries back to
     * PENDING (so they become routable again).
     *
     * <p>{@link DeliveryService#releaseFromRoute(String)} only reverts
     * deliveries currently in ASSIGNED status — deliveries that already moved
     * to OUT_FOR_DELIVERY, DELIVERED, FAILED, or SKIPPED stay where they are.
     * That preserves the work already done if a driver cancels partway
     * through a route they've made progress on.</p>
     *
     * @throws RouteNotFoundException if the id doesn't resolve
     * @throws InvalidRouteTransitionException if the route is already terminal
     */
    public Route cancel(String routeId) {
        Route route = loadRouteOrThrow(routeId);

        if (route.getStatus() != RouteStatus.PLANNED
                && route.getStatus() != RouteStatus.IN_PROGRESS) {
            throw new InvalidRouteTransitionException(
                    route.getStatus(), "cancel", ACTIVE_STATUSES);
        }

        // Flip the route first, then release deliveries. If the release fails
        // after the flip, the route reports CANCELLED but deliveries still
        // point at it — that's a recoverable state (just call
        // releaseFromRoute again) and prevents a "looks like an active route
        // somehow" zombie in the find-active read.
        route.setStatus(RouteStatus.CANCELLED);
        route.setUpdatedAt(TimeUtil.getCurrentDateTime());
        Route saved = routeRepository.save(route);

        try {
            deliveryService.releaseFromRoute(saved.getId());
        } catch (RuntimeException e) {
            // Don't unwind the route flip — better the route be CANCELLED with
            // unreleased deliveries (which the storefront/back-office can fix)
            // than a CANCELLED save that gets reverted, leaving an active
            // route the driver thought they cancelled.
            log.error("Route id={} cancelled but releasing deliveries failed; " +
                    "they may still point at this route until cleaned up", saved.getId(), e);
        }

        log.info("Route id={} cancelled (was {})", saved.getId(), route.getStatus());
        return saved;
    }

    /* ============================================================================
     * Helpers
     * ============================================================================ */

    private Route loadRouteOrThrow(String routeId) {
        if (routeId == null || routeId.isBlank()) {
            throw new RouteNotFoundException(routeId);
        }
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new RouteNotFoundException(routeId));
    }

}
