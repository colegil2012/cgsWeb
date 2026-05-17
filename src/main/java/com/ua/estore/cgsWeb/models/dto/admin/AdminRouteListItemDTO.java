package com.ua.estore.cgsWeb.models.dto.admin;

import com.ua.estore.cgsWeb.models.driver.route.Route;
import com.ua.estore.cgsWeb.models.driver.route.RouteStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Flat projection of {@link Route} for the admin route-list view.
 *
 * <p>Surfaces the denormalized {@code RouteTotals} as plain fields so the
 * list template doesn't reach through nested objects.</p>
 */
@Getter
@Builder
public class AdminRouteListItemDTO {

    private String id;
    private String routeNumber;
    private RouteStatus status;
    private int stopCount;
    private Long distanceMeters;
    private Long durationSeconds;
    private LocalDateTime createdAt;

    public static AdminRouteListItemDTO from(Route route) {
        int stopCount = 0;
        Long distance = null;
        Long duration = null;

        if (route.getTotals() != null) {
            Integer sc = route.getTotals().getStopCount();
            stopCount = (sc != null) ? sc : 0;
            distance = route.getTotals().getDistanceMeters();
            duration = route.getTotals().getDurationSeconds();
        }
        // Fall back to the actual stops list size if totals didn't carry a count.
        if (stopCount == 0 && route.getStops() != null) {
            stopCount = route.getStops().size();
        }

        return AdminRouteListItemDTO.builder()
                .id(route.getId())
                .routeNumber(route.getRouteNumber())
                .status(route.getStatus())
                .stopCount(stopCount)
                .distanceMeters(distance)
                .durationSeconds(duration)
                .createdAt(route.getCreatedAt())
                .build();
    }
}