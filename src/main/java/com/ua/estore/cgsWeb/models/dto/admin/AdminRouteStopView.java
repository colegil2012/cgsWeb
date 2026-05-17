package com.ua.estore.cgsWeb.models.dto.admin;

import com.ua.estore.cgsWeb.models.driver.delivery.Delivery;
import com.ua.estore.cgsWeb.models.driver.delivery.DeliveryStatus;
import com.ua.estore.cgsWeb.models.driver.route.RouteStop;
import com.ua.estore.cgsWeb.models.shop.Order;
import lombok.Builder;
import lombok.Getter;

/**
 * One stop on a route, joined with the data the admin detail page needs.
 *
 * <p>The join chain is {@code RouteStop → Delivery → Order}. Either hop can be
 * absent (a deleted delivery, an order that no longer exists), so every
 * resolved field is nullable and the template guards for "—". The
 * {@code deliveryResolved} / {@code orderResolved} flags let the template show
 * a clear "link broken" state rather than just blank cells.</p>
 */
@Getter
@Builder
public class AdminRouteStopView {

    private int sequence;
    private String deliveryId;

    /* leg metrics straight off the RouteStop */
    private Long legDistanceMeters;
    private Long legDurationSeconds;

    /* resolved Delivery */
    private boolean deliveryResolved;
    private DeliveryStatus deliveryStatus;
    private String customerName;
    private String addressLine;          // "123 Main St, Louisville, KY 40299"
    private String deliveryInstructions;

    /* resolved Order (via Delivery.orderId) */
    private boolean orderResolved;
    private String orderId;
    private String orderNumber;

    /**
     * Build a stop view from the raw stop plus the (possibly null) resolved
     * Delivery and Order.
     */
    public static AdminRouteStopView of(RouteStop stop, Delivery delivery, Order order) {
        AdminRouteStopViewBuilder b = AdminRouteStopView.builder()
                .sequence(stop.getSequence() == null ? 0 : stop.getSequence())
                .deliveryId(stop.getDeliveryId())
                .legDistanceMeters(stop.getLegDistanceMeters())
                .legDurationSeconds(stop.getLegDurationSeconds());

        if (delivery != null) {
            b.deliveryResolved(true)
                    .deliveryStatus(delivery.getStatus())
                    .deliveryInstructions(delivery.getDeliveryInstructions());

            Delivery.CustomerSnapshot c = delivery.getCustomer();
            if (c != null) {
                String full = ((c.getFirstName() == null ? "" : c.getFirstName()) + " "
                        + (c.getLastName() == null ? "" : c.getLastName())).trim();
                b.customerName(full.isEmpty() ? null : full);
            }

            Delivery.AddressSnapshot a = delivery.getAddress();
            if (a != null) {
                b.addressLine(formatAddress(a));
            }
        } else {
            b.deliveryResolved(false);
        }

        if (order != null) {
            b.orderResolved(true)
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber());
        } else {
            b.orderResolved(false);
        }

        return b.build();
    }

    private static String formatAddress(Delivery.AddressSnapshot a) {
        StringBuilder sb = new StringBuilder();
        if (a.getStreet1() != null && !a.getStreet1().isBlank()) sb.append(a.getStreet1());
        if (a.getStreet2() != null && !a.getStreet2().isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(a.getStreet2());
        }
        String cityState = ((a.getCity() == null ? "" : a.getCity())
                + (a.getCity() != null && a.getState() != null ? ", " : "")
                + (a.getState() == null ? "" : a.getState())).trim();
        if (!cityState.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(cityState);
        }
        if (a.getZip() != null && !a.getZip().isBlank()) {
            sb.append(' ').append(a.getZip());
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}