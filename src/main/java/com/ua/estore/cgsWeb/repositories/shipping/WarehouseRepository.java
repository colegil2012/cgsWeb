package com.ua.estore.cgsWeb.repositories.shipping;

import com.ua.estore.cgsWeb.models.shipping.Warehouse;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface WarehouseRepository extends MongoRepository<Warehouse, String> {

    /** The single source of truth for "where do we ship from?" in Phase 1. */
    Optional<Warehouse> findFirstByPrimaryTrueAndActiveTrue();
}