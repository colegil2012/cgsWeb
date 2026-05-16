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

    /**
     * Find the most recent route in any of the given statuses, or empty if
     * none exist. Used by the "active route" concept — pass
     * {@code [PLANNED, IN_PROGRESS]} to find a route that's either ready to
     * start or currently in progress.
     *
     * <p>Sorting by {@code createdAt} descending means the most recently
     * generated route wins if multiple are somehow active. Under the
     * "at most one active route" invariant enforced by
     * {@code RouteGenerationService}, the list should never have more than
     * one — but the ordering makes the read deterministic in case of drift.</p>
     */
    Optional<Route> findFirstByStatusInOrderByCreatedAtDesc(List<RouteStatus> statuses);
}