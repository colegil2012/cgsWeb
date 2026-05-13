package com.ua.estore.cgsWeb.controllers.driver;

import com.ua.estore.cgsWeb.models.dto.driver.DriverOrderDTO;
import com.ua.estore.cgsWeb.services.driver.DriverOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Driver-app HTTP surface. All endpoints under this controller are gated by the
 * {@link com.ua.estore.cgsWeb.security.driver.DriverAuthFilter} bearer-token
 * filter, so handlers can assume the caller is authorized.
 *
 * <p>{@code @CrossOrigin(origins = "*")} is intentionally permissive: the kiosk
 * runs from {@code file://} today (which sends an opaque "null" Origin) and may
 * move to {@code http://localhost} later. Tighten to a specific origin once the
 * deployment story is settled.</p>
 *
 * <p>Endpoints planned but not yet built (see project roadmap):</p>
 * <ul>
 *   <li>{@code PATCH /api/driver/deliveries/&#123;id&#125;/status} — once Delivery doc lands.</li>
 *   <li>{@code POST  /api/driver/gps} — GPS pings from the GPS hat.</li>
 *   <li>{@code GET   /api/driver/route} — server-side route fetch via Google Routes API.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverOrderService driverOrderService;

    /**
     * List all orders in the system, newest first, as {@link DriverOrderDTO}.
     *
     * <p>Currently returns every order regardless of status. Filtering will be
     * added when the Delivery document is in place — at that point this becomes
     * "all orders not yet delivered."</p>
     */
    @GetMapping("/orders")
    public ResponseEntity<List<DriverOrderDTO>> listOrders() {
        List<DriverOrderDTO> orders = driverOrderService.listAllOrders();
        log.info("Driver orders fetched: {} order(s)", orders.size());
        return ResponseEntity.ok(orders);
    }
}