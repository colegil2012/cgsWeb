package com.ua.estore.cgsWeb.exceptions.driver;

import java.util.Map;

/**
 * Thrown when an order included in a route-generate request isn't in PAID
 * status. Only PAID orders can be routed for delivery — PENDING means payment
 * hasn't cleared, CANCELLED/REFUNDED shouldn't be delivered.
 */
public class OrderNotPayableException extends DriverApiException {

    private final Map<String, String> idToStatus;

    public OrderNotPayableException(Map<String, String> idToStatus) {
        super("Orders not in PAID status: " + idToStatus);
        this.idToStatus = Map.copyOf(idToStatus);
    }

    public Map<String, String> getIdToStatus() {
        return idToStatus;
    }
}