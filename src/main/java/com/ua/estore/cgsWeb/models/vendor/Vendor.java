package com.ua.estore.cgsWeb.models.vendor;

import com.ua.estore.cgsWeb.models.address.Address;
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
    private String logoUrl;
    private boolean active;
}
