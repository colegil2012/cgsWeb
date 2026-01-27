package com.ua.estore.cgsWeb.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    private String orderNumber;

    @Field(targetType = FieldType.OBJECT_ID)
    private String userId;
    private String status; // PENDING, PAID, SHIPPED, DELIVERED
    private List<OrderItem> items = new ArrayList<>();
    private OrderTotals totals;
    private LocalDateTime createdAt;

    @Data
    public static class OrderTotals {
        private BigDecimal subtotal;
        private BigDecimal tax;
        private BigDecimal shipping;
        private BigDecimal total;
    }

    @Data
    public static class OrderItem {
        private String productId;
        private String name;
        private BigDecimal priceAtPurchase;
        private Integer quantity;
    }
}
