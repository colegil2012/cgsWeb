package com.ua.estore.cgsWeb.controllers.driver.route;

import com.ua.estore.cgsWeb.models.driver.route.Route;
import com.ua.estore.cgsWeb.models.dto.driver.route.GenerateRouteRequest;
import com.ua.estore.cgsWeb.models.dto.driver.route.RouteDTO;
import com.ua.estore.cgsWeb.models.dto.driver.route.RouteGenerationResult;
import com.ua.estore.cgsWeb.services.driver.route.RouteGenerationService;
import com.ua.estore.cgsWeb.services.driver.route.RouteLifeCycleService;
import com.ua.estore.cgsWeb.services.driver.route.RouteQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Route endpoints. Sits behind the same {@code /api/driver/**} bearer-token
 * filter as the rest of the driver API.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST /api/driver/routes} — generate a route from selected order ids</li>
 *   <li>{@code GET  /api/driver/routes/active} — fetch the currently-active route</li>
 *   <li>{@code GET  /api/driver/routes/&#123;id&#125;} — fetch a specific route by id</li>
 *   <li>{@code POST /api/driver/routes/&#123;id&#125;/start} — PLANNED → IN_PROGRESS</li>
 *   <li>{@code POST /api/driver/routes/&#123;id&#125;/complete} — IN_PROGRESS → COMPLETED</li>
 *   <li>{@code POST /api/driver/routes/&#123;id&#125;/cancel} — release deliveries → CANCELLED</li>
 * </ul>
 *
 * <p>All lifecycle endpoints return the updated {@code RouteDTO} so the
 * kiosk gets one consistent response shape.</p>
 *
 * <p>Path ordering note: {@code /active} is declared before {@code /&#123;id&#125;}
 * so Spring's mapping resolution picks the literal route over the path
 * variable when a request comes in for {@code GET /api/driver/routes/active}.
 * In practice Spring prefers the more specific mapping regardless of order,
 * but declaring the literal first makes the intent unambiguous to a reader.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/driver/routes")
@RequiredArgsConstructor
public class DriverRouteController {

    private final RouteGenerationService routeGenerationService;
    private final RouteQueryService routeQueryService;
    private final RouteLifeCycleService routeLifecycleService;

    /* ============================================================================
     * Generation
     * ============================================================================ */

    @PostMapping
    public ResponseEntity<RouteDTO> generate(@RequestBody GenerateRouteRequest request) {
        RouteGenerationResult result = routeGenerationService.generate(request);
        RouteDTO body = routeQueryService.toDTO(result.route());

        log.info("Route generation: id={} wasCreated={} stops={}",
                result.route().getId(), result.wasCreated(), body.getStops().size());

        if (result.wasCreated()) {
            return ResponseEntity
                    .status(201)
                    .header(HttpHeaders.LOCATION, "/api/driver/routes/" + result.route().getId())
                    .body(body);
        }
        return ResponseEntity.ok(body);
    }

    /* ============================================================================
     * Read
     * ============================================================================ */

    /**
     * The currently-active route (PLANNED or IN_PROGRESS).
     * Returns 404 with no body when no active route exists — the kiosk treats
     * that as "show the empty state on the Route tab" rather than an error.
     */
    @GetMapping("/active")
    public ResponseEntity<RouteDTO> getActive() {
        return routeLifecycleService.findActive()
                .map(routeQueryService::toDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteDTO> getById(@PathVariable("id") String id) {
        return routeQueryService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /* ============================================================================
     * Lifecycle transitions
     * ============================================================================ */

    @PostMapping("/{id}/start")
    public ResponseEntity<RouteDTO> start(@PathVariable("id") String id) {
        Route updated = routeLifecycleService.start(id);
        return ResponseEntity.ok(routeQueryService.toDTO(updated));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<RouteDTO> complete(@PathVariable("id") String id) {
        Route updated = routeLifecycleService.complete(id);
        return ResponseEntity.ok(routeQueryService.toDTO(updated));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<RouteDTO> cancel(@PathVariable("id") String id) {
        Route updated = routeLifecycleService.cancel(id);
        return ResponseEntity.ok(routeQueryService.toDTO(updated));
    }
}