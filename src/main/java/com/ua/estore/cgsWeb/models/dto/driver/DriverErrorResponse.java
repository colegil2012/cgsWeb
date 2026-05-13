package com.ua.estore.cgsWeb.models.dto.driver;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Standard error body returned by the driver API.
 *
 * <p>The {@code error} field is a stable identifier ({@code "OrderNotFound"},
 * {@code "RouteCapacityExceeded"}, etc.) the kiosk can switch on for typed
 * error handling without parsing message strings. {@code message} is human-
 * readable. {@code details} is optional type-specific structured data — e.g.
 * which order ids were missing.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DriverErrorResponse(String error, String message, Map<String, Object> details) {

    public static DriverErrorResponse of(String error, String message) {
        return new DriverErrorResponse(error, message, null);
    }

    public static DriverErrorResponse of(String error, String message, Map<String, Object> details) {
        return new DriverErrorResponse(error, message, details);
    }
}