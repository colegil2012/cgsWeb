package com.ua.estore.cgsWeb.models.dto.roadie;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Package {
    private String length;
    private String width;
    private String height;
    private String weight;
    private int quantity;
}
