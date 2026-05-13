package com.ua.estore.cgsWeb.models.driver.route;

import com.ua.estore.cgsWeb.models.driver.delivery.Delivery;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A planned driving route — an ordered list of {@link RouteStop}s plus the
 * geometry needed to render them on a map.
 *
 * <p><b>Stops reference Deliveries, they don't snapshot them.</b> Per-stop
 * status (PENDING / DELIVERED / etc) lives on {@link Delivery}, not here. The
 * Route is essentially immutable after creation — it's a frozen plan. Status
 * changes happen on the Delivery records that the Route's stops point at.
 * When the kiosk renders the route, the response DTO joins Route → Delivery
 * to get current per-stop status.</p>
 *
 * <p><b>Two status fields:</b></p>
 * <ul>
 *   <li>{@link #status} — the lifecycle of the route itself
 *       (PLANNED → IN_PROGRESS → COMPLETED, or → CANCELLED).</li>
 *   <li>Individual stop statuses live on the referenced Deliveries.</li>
 * </ul>
 *
 * <p><b>Idempotency:</b> the {@link #idempotencyKey} guards against duplicate
 * route creation from double-clicked Generate buttons. The driver app sends a
 * UUID per generation attempt; if a Route with that key already exists, the
 * server returns it instead of creating a new one (and calling the optimizer
 * provider, which costs money/quota).</p>
 */
@Data
@NoArgsConstructor
@Document(collection = "routes")
public class Route {

    @Id
    private String id;

    /**
     * Human-friendly route number for driver paperwork and verbal handoff.
     * Year-prefixed sequential, e.g. {@code "R-2026-00042"}. Generated server-side
     * via the same timestamp-mod trick used by {@code OrderService.generateOrderNumber()};
     * swap for a real Mongo counter collection if collisions ever surface.
     */
    @Indexed(unique = true, sparse = true)
    private String routeNumber;

    /** Lifecycle state of the route. */
    private RouteStatus status;

    /**
     * Idempotency key from the route-generate request. Lets the server detect
     * double-submissions and return the existing route instead of creating a
     * second one. Sparse-unique: nullable in theory, but every Route created
     * via the API should have one.
     */
    @Indexed(unique = true, sparse = true)
    private String idempotencyKey;

    /** Future multi-driver hook — which driver triggered the route generation. */
    @Field(targetType = FieldType.OBJECT_ID)
    private String createdBy;

    /**
     * Where the route starts. Typically the shop's home base, but configurable
     * so we can later support routes that start from a driver's current GPS
     * position mid-day. Lat/lng only; no need for full address structure here.
     */
    private RoutePoint origin;

    /**
     * Stops in optimized order. Sequence numbers are 1-indexed and match the
     * stops' {@code sequence} field. Mongo preserves array order, so this list
     * IS the driver's stop-by-stop plan.
     */
    private List<RouteStop> stops = new ArrayList<>();

    /**
     * Full driving polyline as a GeoJSON LineString. Passed straight to
     * {@code celtechSetRoute()} on the kiosk. Persisted on the Route so
     * re-displaying the route doesn't require re-calling the optimizer.
     */
    private GeoJsonLineString geometry;

    /** Aggregate distance / duration / count, denormalized for cheap display. */
    private RouteTotals totals;

    /** Which optimizer ran, when, with what config. Useful for debugging "why is this route bad". */
    private OptimizationMetadata optimization;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Set when {@link #status} transitions to IN_PROGRESS. */
    private LocalDateTime startedAt;

    /** Set when {@link #status} transitions to COMPLETED. */
    private LocalDateTime completedAt;

    /**
     * A simple {lat, lng} pair. Distinct from address snapshots because routes
     * don't need full street info — just where to draw the line from.
     */
    @Data
    @NoArgsConstructor
    public static class RoutePoint {
        private double latitude;
        private double longitude;

        public RoutePoint(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}