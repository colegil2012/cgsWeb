package com.ua.estore.cgsWeb.services.driver.route;

import com.ua.estore.cgsWeb.models.driver.delivery.Delivery;
import com.ua.estore.cgsWeb.models.driver.route.Route;
import com.ua.estore.cgsWeb.models.driver.route.RouteStop;
import com.ua.estore.cgsWeb.models.dto.driver.route.RouteDTO;
import com.ua.estore.cgsWeb.repositories.driver.delivery.DeliveryRepository;
import com.ua.estore.cgsWeb.repositories.driver.route.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-side service for routes. Loads a {@link Route}, joins in the relevant
 * Deliveries (for per-stop status + display fields), and produces a
 * {@link RouteDTO} for the kiosk.
 *
 * <p>Split from {@code RouteGenerationService} so the read path is uncoupled
 * from the write path. Controllers can call this for the GET endpoint, and
 * also to build the response body for POST after generation completes.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteQueryService {

    private final RouteRepository routeRepository;
    private final DeliveryRepository deliveryRepository;

    /**
     * Load a Route by id and project to a DTO. Returns {@link Optional#empty()}
     * when no Route exists with that id.
     */
    public Optional<RouteDTO> findById(String routeId) {
        return routeRepository.findById(routeId).map(this::toDTO);
    }

    /**
     * Project an already-loaded Route to a DTO. Used by both GET (after
     * findById) and POST (after generation).
     *
     * <p>Loads all referenced Deliveries in one batch query so the DTO build
     * is O(1) round-trips regardless of stop count.</p>
     */
    public RouteDTO toDTO(Route route) {
        if (route == null) return null;

        // Collect referenced delivery ids
        List<String> deliveryIds = route.getStops() == null
                ? List.of()
                : route.getStops().stream().map(RouteStop::getDeliveryId).toList();

        // Single batch load. findAllById is provided by MongoRepository.
        Iterable<Delivery> found = deliveryRepository.findAllById(deliveryIds);
        Map<String, Delivery> deliveriesById = new HashMap<>();
        for (Delivery d : found) {
            deliveriesById.put(d.getId(), d);
        }

        // If any stop's delivery isn't loaded, log it — indicates dangling reference.
        if (route.getStops() != null) {
            for (RouteStop stop : route.getStops()) {
                if (!deliveriesById.containsKey(stop.getDeliveryId())) {
                    log.warn("Route id={} stop sequence={} references missing Delivery id={}",
                            route.getId(), stop.getSequence(), stop.getDeliveryId());
                }
            }
        }

        return RouteDTO.from(route, deliveriesById);
    }
}