package com.ua.estore.cgsWeb.services.shipping;

import com.ua.estore.cgsWeb.models.Address;
import com.ua.estore.cgsWeb.models.shipping.Warehouse;
import com.ua.estore.cgsWeb.repositories.shipping.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Resolves the origin warehouse for outbound deliveries.
 *
 * <p>Phase 1: returns the single {@code primary && active} warehouse.
 * If none exists (e.g. fresh DB, failed seed, admin mistake), a built-in
 * fallback is returned so the shipping estimator never short-circuits —
 * a warning is logged so the condition is visible in ops.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    /** The single warehouse all deliveries ship from in Phase 1. */
    public Warehouse getPrimaryWarehouse() {
        return warehouseRepository.findFirstByPrimaryTrueAndActiveTrue()
                .orElseGet(this::fallbackWarehouse);
    }

    private Warehouse fallbackWarehouse() {
        log.warn("No primary Warehouse found in Mongo — using built-in fallback origin.");

        Address address = Address.builder()
                .street1("4800 State Hwy 1066")
                .street2("")
                .city("Bloomfield")
                .state("KY")
                .zip("40008")
                .latitude(37.949)
                .longitude(-85.235)
                .build();

        Warehouse w = new Warehouse();
        w.setId("fallback");
        w.setName("Celtech General Store");
        w.setSlug("celtech-bloomfield");
        w.setAddress(address);
        w.setPrimary(true);
        w.setActive(true);
        w.setCreatedAt(LocalDateTime.now());
        return w;
    }
}