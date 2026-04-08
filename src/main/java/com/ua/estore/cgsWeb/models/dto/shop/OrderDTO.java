package com.ua.estore.cgsWeb.models.dto.shop;

import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.dto.product.ProductDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    //Order
    private User user;
    private String orderId;
    private UUID idempotencyKey;
    private String description;
    private String status;

    //Shipping
    private List<Shipment> roadieShipments;

    //Payment
    private BigDecimal totalPrice;
    private BigDecimal shippingPrice;
    private BigDecimal subtotalPrice;
    private BigDecimal tax;

    //Cart
    private List<ProductDTO> products;

    @Data
    public static class Shipment {
        private String orderId;
        private String trackingNumber;
    }
}
