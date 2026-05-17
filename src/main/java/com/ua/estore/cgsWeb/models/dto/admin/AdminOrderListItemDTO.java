package com.ua.estore.cgsWeb.models.dto.admin;

import com.ua.estore.cgsWeb.models.shop.Order;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Flat projection of {@link Order} for the admin order-list view.
 *
 * <p>Pulls customer name out of the embedded {@code CustomerSnapshot} into a
 * single display string, and surfaces just the fields the list table needs.</p>
 */
@Getter
@Builder
public class AdminOrderListItemDTO {

    private String id;
    private String orderNumber;
    private String customerName;
    private Order.OrderStatus status;
    private BigDecimal total;
    private int itemCount;
    private LocalDateTime placedAt;

    public static AdminOrderListItemDTO from(Order order) {
        String customerName = "—";
        Order.CustomerSnapshot c = order.getCustomer();
        if (c != null) {
            String full = ((c.getFirstName() == null ? "" : c.getFirstName()) + " "
                    + (c.getLastName() == null ? "" : c.getLastName())).trim();
            if (!full.isEmpty()) customerName = full;
        }

        BigDecimal total = (order.getTotals() != null) ? order.getTotals().getTotal() : null;
        int itemCount = (order.getItems() != null) ? order.getItems().size() : 0;

        return AdminOrderListItemDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(customerName)
                .status(order.getStatus())
                .total(total)
                .itemCount(itemCount)
                .placedAt(order.getPlacedAt())
                .build();
    }
}