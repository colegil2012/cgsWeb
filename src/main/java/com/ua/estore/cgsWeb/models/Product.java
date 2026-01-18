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
    private String description;
    private BigDecimal price;
    private String category;
    private Integer stock;
    private String imageUrl;
    @Field(targetType = FieldType.OBJECT_ID)
    private String vendorId;

    public void increaseStock() {
        this.stock++;
    }

    public void decreaseStock() {
        this.stock--;
    }
}
