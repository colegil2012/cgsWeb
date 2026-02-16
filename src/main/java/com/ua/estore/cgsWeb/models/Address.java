package com.ua.estore.cgsWeb.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Address {

    @Field(targetType = FieldType.OBJECT_ID)
    private String addressId;

    private String type;  //SHIPPING, BILLING
    private String street1;
    private String street2;
    private String city;
    private String state;
    private String zip;
    private double latitude;
    private double longitude;
    private boolean isDefault;
}
