package com.ua.estore.cgsWeb.models.driver.delivery;

/**
 * The result of a single delivery attempt. Distinct from {@link DeliveryStatus}
 * because the overall Delivery can survive multiple failed attempts before
 * landing in a terminal state.
 *
 * <p>Mapping to Delivery status after the attempt is recorded:</p>
 * <ul>
 *   <li>{@link #SUCCESS} → Delivery becomes {@link DeliveryStatus#DELIVERED}</li>
 *   <li>{@link #FAILED}  → Delivery becomes {@link DeliveryStatus#FAILED}, may be retried</li>
 *   <li>{@link #SKIPPED} → Delivery becomes {@link DeliveryStatus#SKIPPED} (terminal)</li>
 * </ul>
 */
public enum DeliveryOutcome {
    SUCCESS,
    FAILED,
    SKIPPED
}