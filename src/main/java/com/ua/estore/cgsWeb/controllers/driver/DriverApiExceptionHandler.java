package com.ua.estore.cgsWeb.controllers.driver;

import com.ua.estore.cgsWeb.exceptions.driver.OrderNotFoundException;
import com.ua.estore.cgsWeb.exceptions.driver.OrderNotPayableException;
import com.ua.estore.cgsWeb.exceptions.driver.RouteCapacityExceededException;
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
 * affected — their own exception handling stays whatever it was. The advice's
 * {@code basePackages} matcher prevents accidental cross-contamination.</p>
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
        // 502 — we couldn't reach or got bad data from the optimization provider.
        // Distinct from 500 (our bug) so the kiosk can show a "service unavailable"
        // message instead of "something broke."
        log.error("RouteOptimization failed", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                DriverErrorResponse.of(
                        "OptimizationFailed",
                        "Route optimization service is unavailable. Try again in a moment.",
                        null
                ));
    }

    /* ============================================================================
     * Framework exceptions
     * ============================================================================ */

    /** Bad JSON, missing required fields, bad enum values, type mismatches in the request body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DriverErrorResponse> handleBadJson(HttpMessageNotReadableException e) {
        log.info("Bad request body: {}", e.getMessage());
        // Don't echo the full stack message back — could leak internal types.
        // Give a generic message; if the kiosk needs to know what's wrong, it
        // should validate before sending.
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                DriverErrorResponse.of(
                        "InvalidRequest",
                        "Request body is malformed or missing required fields",
                        null
                ));
    }

    /** Service-layer guard violations (e.g. RouteGenerationService.validateRequest). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<DriverErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.info("Bad argument: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                DriverErrorResponse.of("InvalidRequest", e.getMessage(), null)
        );
    }

    /** Catch-all. Anything that reaches here is our bug; log loudly. */
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