package com.ua.estore.cgsWeb.controllers.driver;

import com.ua.estore.cgsWeb.exceptions.driver.OrderNotFoundException;
import com.ua.estore.cgsWeb.exceptions.driver.OrderNotPayableException;
import com.ua.estore.cgsWeb.exceptions.driver.RouteCapacityExceededException;
import com.ua.estore.cgsWeb.exceptions.driver.route.InvalidRouteTransitionException;
import com.ua.estore.cgsWeb.exceptions.driver.route.RouteAlreadyActiveException;
import com.ua.estore.cgsWeb.exceptions.driver.route.RouteNotFoundException;
import com.ua.estore.cgsWeb.exceptions.driver.route.RouteOptimizationException;
import com.ua.estore.cgsWeb.models.dto.driver.DriverErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Translates exceptions thrown by driver-API controllers into consistent JSON
 * error responses.
 *
 * <p>Scoped to {@code controllers.driver} so storefront controllers aren't
 * affected — their own exception handling stays whatever it was.</p>
 *
 * <p>Each handler emits a {@link DriverErrorResponse} with a stable {@code error}
 * code the kiosk can switch on, a human-readable {@code message}, and
 * type-specific {@code details} when relevant.</p>
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.ua.estore.cgsWeb.controllers.driver")
public class DriverApiExceptionHandler {

    /* ============================================================================
     * Domain-specific exceptions
     * ============================================================================ */

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<DriverErrorResponse> handleOrderNotFound(OrderNotFoundException e) {
        log.info("OrderNotFound: {}", e.getMissingIds());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                DriverErrorResponse.of(
                        "OrderNotFound",
                        e.getMessage(),
                        Map.of("missingIds", e.getMissingIds())
                ));
    }

    @ExceptionHandler(OrderNotPayableException.class)
    public ResponseEntity<DriverErrorResponse> handleOrderNotPayable(OrderNotPayableException e) {
        log.info("OrderNotPayable: {}", e.getIdToStatus());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                DriverErrorResponse.of(
                        "OrderNotPayable",
                        e.getMessage(),
                        Map.of("ordersByStatus", e.getIdToStatus())
                ));
    }

    @ExceptionHandler(RouteCapacityExceededException.class)
    public ResponseEntity<DriverErrorResponse> handleRouteCapacity(RouteCapacityExceededException e) {
        log.info("RouteCapacityExceeded: {} requested vs {} max", e.getRequested(), e.getMaximum());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                DriverErrorResponse.of(
                        "RouteCapacityExceeded",
                        e.getMessage(),
                        Map.of("requested", e.getRequested(), "maximum", e.getMaximum())
                ));
    }

    @ExceptionHandler(RouteOptimizationException.class)
    public ResponseEntity<DriverErrorResponse> handleOptimizationFailure(RouteOptimizationException e) {
        // 502 — couldn't reach or got bad data from the optimization provider.
        log.error("RouteOptimization failed", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                DriverErrorResponse.of(
                        "OptimizationFailed",
                        "Route optimization service is unavailable. Try again in a moment.",
                        null
                ));
    }

    /* ============================================================================
     * Route lifecycle / active-route guards
     * ============================================================================ */

    /**
     * 409 — a route is already active. The {@code activeRouteId} in the
     * details payload lets the kiosk navigate the driver to the existing
     * route instead of dead-ending them on a failed generate.
     */
    @ExceptionHandler(RouteAlreadyActiveException.class)
    public ResponseEntity<DriverErrorResponse> handleRouteAlreadyActive(RouteAlreadyActiveException e) {
        log.info("RouteAlreadyActive: activeRouteId={}", e.getActiveRouteId());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                DriverErrorResponse.of(
                        "RouteAlreadyActive",
                        e.getMessage(),
                        Map.of("activeRouteId", e.getActiveRouteId())
                ));
    }

    /**
     * 404 — a route-id-by-path endpoint (start/complete/cancel) couldn't find
     * the route. The plain GET endpoints handle their own "not found" with
     * an empty 404; this handler covers the writes.
     */
    @ExceptionHandler(RouteNotFoundException.class)
    public ResponseEntity<DriverErrorResponse> handleRouteNotFound(RouteNotFoundException e) {
        log.info("RouteNotFound: id={}", e.getRouteId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                DriverErrorResponse.of(
                        "RouteNotFound",
                        e.getMessage(),
                        Map.of("routeId", e.getRouteId() == null ? "" : e.getRouteId())
                ));
    }

    /**
     * 409 — the state machine refused the requested transition (e.g.
     * complete on PLANNED, start on COMPLETED). The details carry enough
     * context for the kiosk to show a useful message and disable the
     * buttons that wouldn't work for the current status.
     */
    @ExceptionHandler(InvalidRouteTransitionException.class)
    public ResponseEntity<DriverErrorResponse> handleInvalidTransition(InvalidRouteTransitionException e) {
        log.info("InvalidRouteTransition: currentStatus={} attempted={} allowed={}",
                e.getCurrentStatus(), e.getAttemptedTransition(), e.getAllowedFromStatuses());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                DriverErrorResponse.of(
                        "InvalidRouteTransition",
                        e.getMessage(),
                        Map.of(
                                "currentStatus", e.getCurrentStatus().name(),
                                "attempted", e.getAttemptedTransition(),
                                "allowedFrom", e.getAllowedFromStatuses().stream()
                                        .map(Enum::name)
                                        .toList()
                        )
                ));
    }

    /* ============================================================================
     * Framework exceptions
     * ============================================================================ */

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DriverErrorResponse> handleBadJson(HttpMessageNotReadableException e) {
        log.info("Bad request body: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                DriverErrorResponse.of(
                        "InvalidRequest",
                        "Request body is malformed or missing required fields",
                        null
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<DriverErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.info("Bad argument: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                DriverErrorResponse.of("InvalidRequest", e.getMessage(), null)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DriverErrorResponse> handleUncaught(Exception e) {
        log.error("Unhandled exception in driver API", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                DriverErrorResponse.of(
                        "InternalError",
                        "An unexpected error occurred. Please try again.",
                        null
                ));
    }
}