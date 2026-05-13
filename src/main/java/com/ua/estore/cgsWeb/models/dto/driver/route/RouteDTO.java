package com.ua.estore.cgsWeb.models.dto.driver.route;

import com.ua.estore.cgsWeb.models.driver.delivery.Delivery;
import com.ua.estore.cgsWeb.models.driver.route.Route;
import com.ua.estore.cgsWeb.models.dto.driver.RouteStopDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Kiosk-facing projection of a {@link Route}.
 *
 * <p>Stops are returned as {@link RouteStopDTO}s with current Delivery status
 * already joined in — the kiosk doesn't need to make N additional calls to
 * fetch per-stop status. The service layer is responsible for loading the
 * relevant Deliveries and passing them to {@link #from(Route, Map)}.</p>
 */
@Data
@NoArgsConstructor
public class RouteDTO {

    private String id;
    private String routeNumber;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private OriginDTO origin;

    private List<RouteStopDTO> stops = new ArrayList<>();

    /**
     * GeoJSON LineString shaped for direct consumption by the kiosk's
     * {@code celtechSetRoute()}. The map module expects either a bare
     * geometry or a Feature wrapping a LineString — we use the bare form here.
     */
    private GeometryDTO geometry;

    private TotalsDTO totals;

    /**
     * Build a RouteDTO. {@code deliveriesById} must contain a Delivery for
     * every {@code stop.deliveryId} on the route — the service layer is
     * responsible for ensuring this (a missing delivery here is a bug, not
     * a runtime condition to handle gracefully).
     */
    public static RouteDTO from(Route route, Map<String, Delivery> deliveriesById) {
        if (route == null) return null;

        RouteDTO dto = new RouteDTO();
        dto.setId(route.getId());
        dto.setRouteNumber(route.getRouteNumber());
        dto.setStatus(route.getStatus() != null ? route.getStatus().name() : null);
        dto.setCreatedAt(route.getCreatedAt());
        dto.setStartedAt(route.getStartedAt());
        dto.setCompletedAt(route.getCompletedAt());

        if (route.getOrigin() != null) {
            OriginDTO origin = new OriginDTO();
            origin.setLatitude(route.getOrigin().getLatitude());
            origin.setLongitude(route.getOrigin().getLongitude());
            dto.setOrigin(origin);
        }

        if (route.getStops() != null) {
            dto.setStops(route.getStops().stream()
                    .map(stop -> RouteStopDTO.from(stop, deliveriesById.get(stop.getDeliveryId())))
                    .collect(Collectors.toCollection(ArrayList::new)));
        }

        if (route.getGeometry() != null) {
            GeometryDTO geom = new GeometryDTO();
            geom.setType("LineString");
            geom.setCoordinates(route.getGeometry().getCoordinates());
            dto.setGeometry(geom);
        }

        if (route.getTotals() != null) {
            TotalsDTO totals = new TotalsDTO();
            totals.setDistanceMeters(route.getTotals().getDistanceMeters());
            totals.setDurationSeconds(route.getTotals().getDurationSeconds());
            totals.setStopCount(route.getTotals().getStopCount());
            dto.setTotals(totals);
        }

        return dto;
    }

    @Data @NoArgsConstructor
    public static class OriginDTO {
        private double latitude;
        private double longitude;
    }

    @Data @NoArgsConstructor
    public static class TotalsDTO {
        private Long distanceMeters;
        private Long durationSeconds;
        private Integer stopCount;
    }

    /**
     * Bare GeoJSON LineString. The kiosk's celtechSetRoute() accepts either
     * this or a Feature wrapping it; the bare form is simpler.
     */
    @Data @NoArgsConstructor
    public static class GeometryDTO {
        private String type;
        private List<double[]> coordinates;
    }
}