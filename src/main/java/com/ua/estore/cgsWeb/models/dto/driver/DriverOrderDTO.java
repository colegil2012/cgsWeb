package com.ua.estore.cgsWeb.models.dto.driver;

import com.ua.estore.cgsWeb.models.driver.delivery.Delivery;
import com.ua.estore.cgsWeb.models.shop.Order;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Driver-app projection of an {@link Order}. Carries only the fields the kiosk
 * needs to render the orders tab and (eventually) plot stops on the map.
 *
 * <p>Now carries an associated {@link #deliveryId} when a Delivery exists for
 * the order — the kiosk uses this to track which deliveries are part of any
 * future route. The {@code deliveryStatus} field surfaces current logistical
 * state without an extra round trip.</p>
 *
 * <p>Excluded on purpose:</p>
 * <ul>
 *   <li>{@code userId}, {@code idempotencyKey}, {@code squareData} — internal plumbing.</li>
 *   <li>{@code billTo} — the driver doesn't need billing info.</li>
 *   <li>Customer email — not needed for delivery, avoids leaking PII through the kiosk endpoint.</li>
 *   <li>{@code paymentStatus} — payment state is the storefront's concern.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
public class DriverOrderDTO {

    private String id;
    private String orderNumber;
    private String status;             // financial status, exposed for visibility

    /** Associated Delivery id when one exists. Null until the Delivery doc is created. */
    private String deliveryId;

    /** Current Delivery status. Null when no Delivery exists yet. */
    private String deliveryStatus;

    private String customerName;
    private String customerPhone;
    private ShipTo shipTo;
    private String deliveryInstructions;
    private List<Item> items = new ArrayList<>();
    private BigDecimal total;
    private LocalDateTime placedAt;

    @Data
    @NoArgsConstructor
    public static class ShipTo {
        private String street1;
        private String street2;
        private String city;
        private String state;
        private String zip;
        private double latitude;
        private double longitude;
    }

    @Data
    @NoArgsConstructor
    public static class Item {
        private String name;
        private String vendorName;
        private Integer quantity;
        private String itemNote;
    }

    // ====================================================================
    // Mapping
    // ====================================================================

    /**
     * Build from an Order with no Delivery yet. {@link #deliveryId} and
     * {@link #deliveryStatus} will be null.
     */
    public static DriverOrderDTO from(Order order) {
        return from(order, null);
    }

    /**
     * Build from an Order with its associated Delivery (may be null if not
     * yet created). When the Delivery is present, fills in {@link #deliveryId}
     * and {@link #deliveryStatus}.
     */
    public static DriverOrderDTO from(Order order, Delivery delivery) {
        if (order == null) return null;

        DriverOrderDTO dto = new DriverOrderDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        dto.setDeliveryInstructions(order.getDeliveryInstructions());
        dto.setPlacedAt(order.getPlacedAt());

        // Customer — concat first+last, drop email
        Order.CustomerSnapshot c = order.getCustomer();
        if (c != null) {
            String first = c.getFirstName() != null ? c.getFirstName() : "";
            String last  = c.getLastName()  != null ? c.getLastName()  : "";
            String name  = (first + " " + last).trim();
            dto.setCustomerName(name.isEmpty() ? null : name);
            dto.setCustomerPhone(c.getPhone());
        }

        Order.AddressSnapshot s = order.getShipTo();
        if (s != null) {
            ShipTo ship = new ShipTo();
            ship.setStreet1(s.getStreet1());
            ship.setStreet2(s.getStreet2());
            ship.setCity(s.getCity());
            ship.setState(s.getState());
            ship.setZip(s.getZip());
            ship.setLatitude(s.getLatitude());
            ship.setLongitude(s.getLongitude());
            dto.setShipTo(ship);
        }

        if (order.getItems() != null) {
            List<Item> items = new ArrayList<>(order.getItems().size());
            for (Order.OrderItem oi : order.getItems()) {
                Item item = new Item();
                item.setName(oi.getName());
                item.setVendorName(oi.getVendorName());
                item.setQuantity(oi.getQuantity());
                item.setItemNote(oi.getItemNote());
                items.add(item);
            }
            dto.setItems(items);
        }

        if (order.getTotals() != null) {
            dto.setTotal(order.getTotals().getTotal());
        }

        // Delivery is the source of truth for status; surface it when available.
        if (delivery != null) {
            dto.setDeliveryId(delivery.getId());
            dto.setDeliveryStatus(delivery.getStatus() != null ? delivery.getStatus().name() : null);
        }

        return dto;
    }
}