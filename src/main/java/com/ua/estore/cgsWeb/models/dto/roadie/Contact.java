package com.ua.estore.cgsWeb.models.dto.roadie;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Contact {
    private String name;
    private String phone;
}
