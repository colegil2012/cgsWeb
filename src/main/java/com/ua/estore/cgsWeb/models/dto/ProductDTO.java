package com.ua.estore.cgsWeb.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ProductDTO implements Serializable {
    private String id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String categoryName;
    private String imageUrl;
    private String vendorName;
    private int stock;
    private int lowStockThreshold;
    private int quantity;

    public void increaseQuantity() {
        this.quantity++;
    }

    public void decreaseQuantity() {
        this.quantity--;
    }
}
