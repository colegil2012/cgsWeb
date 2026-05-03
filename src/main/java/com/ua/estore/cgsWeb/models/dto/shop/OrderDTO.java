package com.ua.estore.cgsWeb.models.dto.shop;

import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.models.dto.product.ProductDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
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

    //Cart
    private List<ProductDTO> products;
}
