package com.ua.estore.cgsWeb.controllers.driver.delivery;

import com.ua.estore.cgsWeb.models.driver.delivery.Delivery;
import com.ua.estore.cgsWeb.models.driver.delivery.DeliveryOutcome;
import com.ua.estore.cgsWeb.models.dto.driver.delivery.DeliveryStatusUpdateRequest;
import com.ua.estore.cgsWeb.models.shop.Order;
import com.ua.estore.cgsWeb.services.driver.delivery.DeliveryService;
import com.ua.estore.cgsWeb.services.shop.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Delivery endpoints. Sits behind the same {@code /api/driver/**}
 * bearer-token filter as the rest of the driver API.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code PATCH /api/driver/deliveries/&#123;id&#125;/status} —
 *       record an attempt outcome. Triggers status flip, attempt history
 *       append, and (on SUCCESS) Order.deliveredAt denormalization.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/driver/deliveries")
@RequiredArgsConstructor
public class DriverDeliveryController {

    private final DeliveryService deliveryService;
    private final OrderService orderService;

    @PatchMapping("/{id}/status")
    public ResponseEntity<DeliveryStatusResponse> updateStatus(
            @PathVariable("id") String deliveryId,
            @RequestBody DeliveryStatusUpdateRequest body) {

        if (body == null || body.getOutcome() == null) {
            return ResponseEntity.badRequest().build();
        }

        // The service throws IllegalArgumentException if the delivery doesn't
        // exist; that maps to 400 via the exception handler. We could instead
        // pre-check with findById and 404 — preference for the cleaner status
        // code over the cleaner service signature.
        if (deliveryService.findById(deliveryId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // driverId is null for now (owner-operator phase). Once per-driver auth
        // lands, pull from SecurityContextHolder.getContext().getAuthentication().
        DeliveryOutcome outcome = DeliveryOutcome.valueOf(String.valueOf(body.getOutcome()));
        Delivery updated = deliveryService.recordOutcome(
                deliveryId,
                outcome,
                body.getNotes(),
                null /* driverId */
        );

        Order updatedOrder = orderService.completeDelivery(
                updated.getOrderId(), updated.getCustomer().getUserId());

        log.info("Delivery {} updated: outcome={} status={}",
                updated.getId(), outcome, updated.getStatus());

        log.info("Order {} updated: deliveredAt={}", updatedOrder.getId(), updatedOrder.getDeliveredAt());

        DeliveryStatusResponse response = new DeliveryStatusResponse(
                updated.getId(),
                updated.getStatus().name(),
                updated.getDeliveredAt(),
                updated.getAttempts() != null ? updated.getAttempts().size() : 0
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Minimal response so the kiosk can update its UI without a re-fetch.
     * If the kiosk needs more fields later, this can grow into a full DTO.
     */
    public record DeliveryStatusResponse(
            String deliveryId,
            String status,
            LocalDateTime deliveredAt,
            int attemptCount
    ) {}
}