package com.ua.estore.cgsWeb.models.dto.driver.delivery;

import com.ua.estore.cgsWeb.models.driver.delivery.DeliveryOutcome;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code PATCH /api/driver/deliveries/&#123;id&#125;/status}.
 *
 * <p>The driver app sends one of these whenever a delivery's outcome is
 * recorded — successful delivery, failure, or skip. The service layer:</p>
 * <ol>
 *   <li>Appends an entry to {@code Delivery.attempts} with this outcome.</li>
 *   <li>Updates {@code Delivery.status} to match (e.g. SUCCESS → DELIVERED).</li>
 *   <li>If status becomes DELIVERED, sets {@code Delivery.deliveredAt} and
 *       denormalizes that to the source Order.</li>
 * </ol>
 *
 * <p>The {@link #outcome} field is typed as the enum directly. Jackson rejects
 * unknown values with {@code HttpMessageNotReadableException}, which the
 * driver-API exception handler maps to a 400 with a clean error body.</p>
 */
@Data
@NoArgsConstructor
public class DeliveryStatusUpdateRequest {

    /** SUCCESS, FAILED, or SKIPPED. Case-sensitive. */
    private DeliveryOutcome outcome;

    /** Optional driver-supplied notes captured on the attempt record. */
    private String notes;
}