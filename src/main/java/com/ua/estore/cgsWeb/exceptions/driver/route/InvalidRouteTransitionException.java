package com.ua.estore.cgsWeb.exceptions.driver.route;

import com.ua.estore.cgsWeb.models.driver.route.RouteStatus;
import lombok.Getter;

import java.util.List;

/**
 * Thrown when a requested route state transition isn't allowed by the
 * state machine. Examples:
 * <ul>
 *   <li>Calling complete on a PLANNED route (must START first)</li>
 *   <li>Calling start on a COMPLETED route (terminal state)</li>
 *   <li>Any transition out of a terminal state (COMPLETED, CANCELLED)</li>
 * </ul>
 *
 * <p>Carries enough context for the kiosk to render a useful message
 * ("Can't complete a PLANNED route. Start it first.") and to disable the
 * buttons that wouldn't work for the current status.</p>
 *
 * <p>Maps to HTTP 409 Conflict.</p>
 */
@Getter
public class InvalidRouteTransitionException extends RuntimeException {

    private final RouteStatus currentStatus;
    private final String attemptedTransition;
    private final List<RouteStatus> allowedFromStatuses;

    public InvalidRouteTransitionException(RouteStatus currentStatus,
                                           String attemptedTransition,
                                           List<RouteStatus> allowedFromStatuses) {
        super(String.format(
                "Cannot %s a route in %s status (allowed from: %s)",
                attemptedTransition, currentStatus, allowedFromStatuses));
        this.currentStatus = currentStatus;
        this.attemptedTransition = attemptedTransition;
        this.allowedFromStatuses = allowedFromStatuses;
    }
}