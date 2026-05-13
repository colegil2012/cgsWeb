package com.ua.estore.cgsWeb.controllers.driver.route;

import com.ua.estore.cgsWeb.models.dto.driver.route.GenerateRouteRequest;
import com.ua.estore.cgsWeb.models.dto.driver.route.RouteDTO;
import com.ua.estore.cgsWeb.models.dto.driver.route.RouteGenerationResult;
import com.ua.estore.cgsWeb.services.driver.route.RouteGenerationService;
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
 *   <li>{@code GET /api/driver/routes/&#123;id&#125;} — fetch a previously-generated route</li>
 * </ul>
 *
 * <p>POST returns {@code 201 Created} when a new Route was built, or
 * {@code 200 OK} when an existing Route matched via the idempotency key. The
 * kiosk can use this distinction to decide whether to navigate the user
 * somewhere or just re-display the existing route.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/driver/routes")
@RequiredArgsConstructor
public class DriverRouteController {

    private final RouteGenerationService routeGenerationService;
    private final RouteQueryService routeQueryService;

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

    @GetMapping("/{id}")
    public ResponseEntity<RouteDTO> getById(@PathVariable("id") String id) {
        return routeQueryService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}