package com.ua.estore.cgsWeb.models.dto.driver;

import com.ua.estore.cgsWeb.models.driver.delivery.Delivery;
import com.ua.estore.cgsWeb.models.driver.route.RouteStop;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-stop projection. Built by joining a {@link RouteStop} with the referenced
 * {@link Delivery}. Status comes from the Delivery — Route doesn't own status.
 *
 * <p>All display fields (customer, address, instructions) come from the
 * Delivery's snapshots, which themselves came from the Order at Delivery
 * creation time. So the chain is Order → Delivery → DTO. The Route record
 * never has to carry display fields.</p>
 */
@Data
@NoArgsConstructor
public class RouteStopDTO {

    private Integer sequence;

    private String deliveryId;
    private String orderId;
    private String orderNumber;

    private String customerName;
    private String customerPhone;

    private AddressDTO address;

    private String deliveryInstructions;

    /** Current Delivery status — read from the Delivery, not stored on the Route. */
    private String status;

    /** How many attempts have been made on this Delivery. Useful for "2nd try" UI hints. */
    private Integer attemptCount;

    private Long legDistanceMeters;
    private Long legDurationSeconds;

    public static RouteStopDTO from(RouteStop stop, Delivery delivery) {
        if (stop == null) return null;

        RouteStopDTO dto = new RouteStopDTO();
        dto.setSequence(stop.getSequence());
        dto.setDeliveryId(stop.getDeliveryId());
        dto.setLegDistanceMeters(stop.getLegDistanceMeters());
        dto.setLegDurationSeconds(stop.getLegDurationSeconds());

        // Delivery should always be present per the service-layer contract.
        // Defensively guarded so a stale stop doesn't NPE the whole response —
        // but log-worthy if it happens.
        if (delivery != null) {
            dto.setOrderId(delivery.getOrderId());
            dto.setOrderNumber(delivery.getOrderNumber());
            dto.setStatus(delivery.getStatus() != null ? delivery.getStatus().name() : null);
            dto.setAttemptCount(delivery.getAttempts() != null ? delivery.getAttempts().size() : 0);
            dto.setDeliveryInstructions(delivery.getDeliveryInstructions());

            Delivery.CustomerSnapshot c = delivery.getCustomer();
            if (c != null) {
                String first = c.getFirstName() != null ? c.getFirstName() : "";
                String last  = c.getLastName()  != null ? c.getLastName()  : "";
                String name  = (first + " " + last).trim();
                dto.setCustomerName(name.isEmpty() ? null : name);
                dto.setCustomerPhone(c.getPhone());
            }

            Delivery.AddressSnapshot a = delivery.getAddress();
            if (a != null) {
                AddressDTO addr = new AddressDTO();
                addr.setStreet1(a.getStreet1());
                addr.setStreet2(a.getStreet2());
                addr.setCity(a.getCity());
                addr.setState(a.getState());
                addr.setZip(a.getZip());
                addr.setLatitude(a.getLatitude());
                addr.setLongitude(a.getLongitude());
                dto.setAddress(addr);
            }
        }

        return dto;
    }

    @Data @NoArgsConstructor
    public static class AddressDTO {
        private String street1;
        private String street2;
        private String city;
        private String state;
        private String zip;
        private double latitude;
        private double longitude;
    }
}