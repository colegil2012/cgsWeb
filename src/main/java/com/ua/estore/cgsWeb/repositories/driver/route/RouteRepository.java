package com.ua.estore.cgsWeb.repositories.driver.route;

import com.ua.estore.cgsWeb.models.driver.route.Route;
import com.ua.estore.cgsWeb.models.driver.route.RouteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends MongoRepository<Route, String> {

    /* ---- Existing driver queries (unchanged) ---- */

    Optional<Route> findByIdempotencyKey(String idempotencyKey);

    List<Route> findByStatus(RouteStatus status);

    Optional<Route> findFirstByStatusInOrderByCreatedAtDesc(List<RouteStatus> statuses);

    /* ============================================================================
     * Admin queries (Round 2)
     * ============================================================================ */

    /**
     * Paged list of all routes — the admin routes list, unfiltered.
     * Most-recent first so the newest routes are on page one.
     */
    Page<Route> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Paged list filtered to a single route status, most-recent first. */
    Page<Route> findByStatusOrderByCreatedAtDesc(RouteStatus status, Pageable pageable);
}