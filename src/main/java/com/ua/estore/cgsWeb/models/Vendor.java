package com.ua.estore.cgsWeb.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "vendors")
public class Vendor {
    @Id
    private String id;
    private String name;
    private String description;
    private String address_1;
    private String address_2;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String logo_url;
}
