package com.ua.estore.cgsWeb.driver.route;

import com.ua.estore.cgsWeb.config.props.RouteProperties;
import com.ua.estore.cgsWeb.exceptions.driver.route.RouteOptimizationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenRouteService (ORS) implementation of {@link RouteOptimizer}.
 *
 * <p>ORS splits the work across two endpoints:</p>
 * <ol>
 *   <li>{@code /optimization} — VROOM solver, returns the optimal stop order</li>
 *   <li>{@code /v2/directions/{profile}/geojson} — turn-by-turn directions
 *       for the waypoints in that order, returns the polyline geometry</li>
 * </ol>
 *
 * <p>We call both in sequence. The first call determines the order; the second
 * call produces the line we draw on the map. The {@link OptimizedRoute} carries
 * both as one object.</p>
 *
 * <p><b>VROOM uses integer job/vehicle ids.</b> We assign sequential integers
 * to each waypoint, send those to ORS, then translate the result back to
 * Delivery ids. The {@link #idToDeliveryId} map below is built fresh per call;
 * no shared state between requests.</p>
 *
 * <p><b>Free-tier rate limits:</b> ORS publishes 40 req/min for optimization
 * and directions on the free tier. The service layer cache (Route doc with
 * idempotencyKey) takes most of the pressure off; under heavy load a paid plan
 * or a self-hosted ORS instance would be the upgrade path.</p>
 *
 * <p>Activated when {@code celtech.route.optimizer = ors} (the default — see
 * application.yml).</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "celtech.route.optimizer", havingValue = "ors", matchIfMissing = true)
public class OrsRouteOptimizer implements RouteOptimizer {

    private static final String NAME = "ors-vroom";
    private static final String GEOMETRY_PROVIDER = "ors-directions";

    /**
     * Practical upper bound for the free tier. VROOM itself scales to thousands
     * of jobs given enough time, but free-tier rate limits and 30s timeouts
     * make 100 a reasonable kiosk-side cap. Tune up if you self-host ORS.
     */
    private static final int MAX_WAYPOINTS = 100;

    private final RouteProperties props;
    private final RestClient http;

    public OrsRouteOptimizer(RouteProperties props) {
        this.props = props;
        RouteProperties.Ors ors = props.ors();
        if (ors == null || ors.baseUrl() == null) {
            throw new IllegalStateException(
                    "RouteProperties.ors.baseUrl is required when celtech.route.optimizer = ors");
        }
        this.http = RestClient.builder()
                .baseUrl(ors.baseUrl())
                .build();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getMaxWaypoints() {
        return MAX_WAYPOINTS;
    }

    @Override
    public OptimizedRoute optimize(RoutePoint origin, List<RouteWaypoint> waypoints, boolean returnToOrigin) {
        if (waypoints == null || waypoints.isEmpty()) {
            throw new RouteOptimizationException("Cannot optimize a route with no waypoints");
        }
        if (waypoints.size() > MAX_WAYPOINTS) {
            throw new RouteOptimizationException(
                    "Too many waypoints: " + waypoints.size() + " (max " + MAX_WAYPOINTS + ")");
        }

        String apiKey = props.ors() != null ? props.ors().apiKey() : null;
        if (apiKey == null || apiKey.isBlank()) {
            throw new RouteOptimizationException("ORS API key is not configured");
        }

        long start = System.currentTimeMillis();

        // Assign integer ids for VROOM, keep a reverse map.
        Map<Integer, String> idToDeliveryId = new HashMap<>(waypoints.size());
        for (int i = 0; i < waypoints.size(); i++) {
            idToDeliveryId.put(i + 1, waypoints.get(i).deliveryId());
        }

        // Step 1: ask VROOM for the optimal order
        OrsOptimizationResponse opt = callOptimization(apiKey, origin, waypoints, returnToOrigin);
        List<Integer> orderedJobIds = extractOrderedJobIds(opt);

        // Step 2: ask Directions for the polyline through the optimized stops
        List<RouteWaypoint> orderedWaypoints = new ArrayList<>(orderedJobIds.size());
        for (Integer jobId : orderedJobIds) {
            String deliveryId = idToDeliveryId.get(jobId);
            orderedWaypoints.add(findWaypoint(waypoints, deliveryId));
        }

        OrsDirectionsResponse dir = callDirections(apiKey, origin, orderedWaypoints, returnToOrigin);

        long elapsed = System.currentTimeMillis() - start;

        return buildOptimizedRoute(orderedWaypoints, dir, elapsed);
    }

    // ====================================================================
    // ORS call: /optimization (VROOM)
    // ====================================================================

    private OrsOptimizationResponse callOptimization(String apiKey,
                                                     RoutePoint origin,
                                                     List<RouteWaypoint> waypoints,
                                                     boolean returnToOrigin) {
        // Build VROOM job + vehicle structures. VROOM expects [lng, lat].
        List<Map<String, Object>> jobs = new ArrayList<>(waypoints.size());
        for (int i = 0; i < waypoints.size(); i++) {
            RouteWaypoint w = waypoints.get(i);
            jobs.add(Map.of(
                    "id", i + 1,
                    "location", new double[]{w.longitude(), w.latitude()}
            ));
        }

        Map<String, Object> vehicle = new HashMap<>();
        vehicle.put("id", 1);
        vehicle.put("profile", profile());
        vehicle.put("start", new double[]{origin.longitude(), origin.latitude()});
        if (returnToOrigin) {
            vehicle.put("end", new double[]{origin.longitude(), origin.latitude()});
        }

        Map<String, Object> body = Map.of(
                "jobs", jobs,
                "vehicles", List.of(vehicle)
        );

        try {
            return http.post()
                    .uri("/optimization")
                    .header(HttpHeaders.AUTHORIZATION, apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .retrieve()
                    .body(OrsOptimizationResponse.class);
        } catch (RestClientException e) {
            throw new RouteOptimizationException("ORS optimization request failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Integer> extractOrderedJobIds(OrsOptimizationResponse response) {
        if (response == null || response.routes == null || response.routes.isEmpty()) {
            throw new RouteOptimizationException("ORS optimization returned no routes");
        }
        Map<String, Object> firstRoute = response.routes.get(0);
        List<Map<String, Object>> steps = (List<Map<String, Object>>) firstRoute.get("steps");
        if (steps == null) {
            throw new RouteOptimizationException("ORS optimization response missing steps");
        }

        List<Integer> ordered = new ArrayList<>();
        for (Map<String, Object> step : steps) {
            // VROOM step types: "start", "job", "end". We only want "job" entries.
            if ("job".equals(step.get("type"))) {
                Object id = step.get("id");
                if (id instanceof Number n) {
                    ordered.add(n.intValue());
                }
            }
        }
        if (ordered.isEmpty()) {
            throw new RouteOptimizationException("ORS optimization returned no job steps");
        }
        return ordered;
    }

    // ====================================================================
    // ORS call: /v2/directions/{profile}/geojson
    // ====================================================================

    private OrsDirectionsResponse callDirections(String apiKey,
                                                 RoutePoint origin,
                                                 List<RouteWaypoint> orderedWaypoints,
                                                 boolean returnToOrigin) {
        // Build coordinate array. Always start with origin; append ordered
        // waypoints; optionally end with origin again.
        List<double[]> coords = new ArrayList<>(orderedWaypoints.size() + 2);
        coords.add(new double[]{origin.longitude(), origin.latitude()});
        for (RouteWaypoint w : orderedWaypoints) {
            coords.add(new double[]{w.longitude(), w.latitude()});
        }
        if (returnToOrigin) {
            coords.add(new double[]{origin.longitude(), origin.latitude()});
        }

        Map<String, Object> body = Map.of(
                "coordinates", coords,
                "instructions", true,
                "units", "m"
        );

        String uri = "/v2/directions/" + profile() + "/geojson";
        try {
            return http.post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT, "application/geo+json")
                    .body(body)
                    .retrieve()
                    .body(OrsDirectionsResponse.class);
        } catch (RestClientException e) {
            throw new RouteOptimizationException("ORS directions request failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private OptimizedRoute buildOptimizedRoute(List<RouteWaypoint> orderedWaypoints,
                                               OrsDirectionsResponse dir,
                                               long elapsedMillis) {
        if (dir == null || dir.features == null || dir.features.isEmpty()) {
            throw new RouteOptimizationException("ORS directions returned no features");
        }
        Map<String, Object> feature = dir.features.get(0);
        Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
        Map<String, Object> properties = (Map<String, Object>) feature.get("properties");

        if (geometry == null || properties == null) {
            throw new RouteOptimizationException("ORS directions feature missing geometry/properties");
        }

        // GeoJSON coordinates: list of [lng, lat] pairs (sometimes with elevation as third element).
        List<List<Double>> rawCoords = (List<List<Double>>) geometry.get("coordinates");
        List<double[]> coords = new ArrayList<>(rawCoords.size());
        for (List<Double> pt : rawCoords) {
            // Force to 2D even if elevation slipped in.
            coords.add(new double[]{pt.get(0), pt.get(1)});
        }

        // Summary holds totals; segments[] holds per-leg metrics.
        Map<String, Object> summary = (Map<String, Object>) properties.get("summary");
        long totalDistance = summary != null ? asLong(summary.get("distance")) : 0;
        long totalDuration = summary != null ? asLong(summary.get("duration")) : 0;

        List<Map<String, Object>> segments = (List<Map<String, Object>>) properties.get("segments");

        // Map segments back to OptimizedStops. ORS returns one segment per leg
        // between consecutive coordinate inputs. Our coordinates were:
        //   [origin, w1, w2, ..., wN]  (without return)  → N segments
        //   [origin, w1, w2, ..., wN, origin]  (with)    → N+1 segments
        // We want one OptimizedStop per waypoint, with leg = segment leading TO that waypoint.
        List<OptimizedStop> stops = new ArrayList<>(orderedWaypoints.size());
        for (int i = 0; i < orderedWaypoints.size(); i++) {
            RouteWaypoint w = orderedWaypoints.get(i);
            long legDist = 0;
            long legDur  = 0;
            if (segments != null && i < segments.size()) {
                Map<String, Object> seg = segments.get(i);
                legDist = asLong(seg.get("distance"));
                legDur  = asLong(seg.get("duration"));
            }
            stops.add(new OptimizedStop(w.deliveryId(), i + 1, legDist, legDur));
        }

        // ORS version string lives in metadata when present; not critical.
        String version = null;
        Object meta = dir.metadata;
        if (meta instanceof Map<?, ?> m) {
            Object engine = m.get("engine");
            if (engine instanceof Map<?, ?> em) {
                Object v = em.get("version");
                if (v != null) version = v.toString();
            }
        }

        return new OptimizedRoute(
                stops,
                coords,
                totalDistance,
                totalDuration,
                GEOMETRY_PROVIDER,
                version,
                elapsedMillis
        );
    }

    // ====================================================================
    // Helpers
    // ====================================================================

    private String profile() {
        String p = props.ors() != null ? props.ors().profile() : null;
        return (p != null && !p.isBlank()) ? p : "driving-car";
    }

    private RouteWaypoint findWaypoint(List<RouteWaypoint> all, String deliveryId) {
        for (RouteWaypoint w : all) {
            if (w.deliveryId().equals(deliveryId)) return w;
        }
        // Should never happen — VROOM only returns ids we sent.
        throw new RouteOptimizationException(
                "Optimizer returned unknown delivery id: " + deliveryId);
    }

    private static long asLong(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.longValue();
        return 0;
    }

    // ====================================================================
    // Response shapes — minimal, just enough to extract what we need
    // ====================================================================

    /**
     * VROOM optimization response. {@code routes[0].steps[]} carries the
     * ordered job ids. We use a typed-but-loose shape because VROOM returns
     * many fields we don't care about.
     */
    public static class OrsOptimizationResponse {
        public List<Map<String, Object>> routes;
        public Object summary;
        public List<Map<String, Object>> unassigned;
    }

    /**
     * ORS directions response — GeoJSON FeatureCollection. We extract the
     * single Feature, then dig into geometry.coordinates and properties.summary.
     */
    public static class OrsDirectionsResponse {
        public List<Map<String, Object>> features;
        public Object metadata;
        public Object bbox;
        public String type;
    }
}