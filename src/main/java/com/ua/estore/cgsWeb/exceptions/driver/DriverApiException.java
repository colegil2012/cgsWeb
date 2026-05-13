package com.ua.estore.cgsWeb.exceptions.driver;

/**
 * Base exception for all driver-API failures. Subclasses are mapped to HTTP
 * status codes in the controller's exception handler.
 */
public class DriverApiException extends RuntimeException {

    public DriverApiException(String message) {
        super(message);
    }

    public DriverApiException(String message, Throwable cause) {
        super(message, cause);
    }
}