package com.ua.estore.cgsWeb.exceptions.driver;

import java.util.List;

/** Thrown when an order id supplied to the API doesn't exist in the database. */
public class OrderNotFoundException extends DriverApiException {

    private final List<String> missingIds;

    public OrderNotFoundException(List<String> missingIds) {
        super("Orders not found: " + missingIds);
        this.missingIds = List.copyOf(missingIds);
    }

    public List<String> getMissingIds() {
        return missingIds;
    }
}