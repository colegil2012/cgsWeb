package com.ua.estore.cgsWeb.exceptions.driver;

/**
 * Thrown when a route generation request includes more waypoints than the
 * configured optimizer can handle. ORS via VROOM scales to hundreds of stops;
 * Google Routes API caps at 25. The optimizer implementation defines its own
 * limit and throws this when exceeded.
 */
public class RouteCapacityExceededException extends DriverApiException {

    private final int requested;
    private final int maximum;

    public RouteCapacityExceededException(int requested, int maximum) {
        super("Route has " + requested + " waypoints; optimizer maximum is " + maximum);
        this.requested = requested;
        this.maximum = maximum;
    }

    public int getRequested() { return requested; }
    public int getMaximum()   { return maximum;   }
}