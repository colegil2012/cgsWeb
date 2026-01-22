package com.ua.estore.cgsWeb.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "vendors")
public class Vendor {
    @Id
    private String id;
    private String name;
    private String slug;
    private String description;
    private List<Address> addresses = new ArrayList<>();
    private String logo_url;
    private int lead_time;
    private boolean active;

    @Data
    public static class Address {
        private String type;
        private String street;
        private String city;
        private String state;
        private String zip;
        private boolean isDefault;
    }
}
