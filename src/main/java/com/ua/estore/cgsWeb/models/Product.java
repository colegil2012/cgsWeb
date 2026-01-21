package com.ua.estore.cgsWeb.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;
    private String name;
    private String slug;
    private String sku;
    private String description;
    private BigDecimal price;
    private BigDecimal salePrice;

    @Field(targetType = FieldType.OBJECT_ID)
    private String categoryId;

    private Integer stock;
    private Integer lowStockThreshold;
    private String imageUrl;
    private boolean active;

    @Field(targetType = FieldType.OBJECT_ID)
    private String vendorId;

    private ProductAttributes attributes;
    private int readyTime;

    @Data
    public static class ProductAttributes {
        private Double length;
        private Double width;
        private Double height;
        private String weight;
        private String type;
    }



    public void increaseStock() {
        this.stock++;
    }

    public void decreaseStock() {
        this.stock--;
    }
}
