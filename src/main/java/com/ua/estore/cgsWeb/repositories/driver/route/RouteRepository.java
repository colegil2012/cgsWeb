package com.ua.estore.cgsWeb.repositories.driver.route;

import com.ua.estore.cgsWeb.models.driver.route.Route;
import com.ua.estore.cgsWeb.models.driver.route.RouteStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends MongoRepository<Route, String> {

    /**
     * Idempotency check on route creation. The driver app sends a UUID per
     * generation attempt; if a Route with that key already exists, we return
     * it instead of calling the optimizer again.
     */
    Optional<Route> findByIdempotencyKey(String idempotencyKey);

    /** "Show me all currently-active routes" — drives the multi-driver dispatcher view later. */
    List<Route> findByStatus(RouteStatus status);
}