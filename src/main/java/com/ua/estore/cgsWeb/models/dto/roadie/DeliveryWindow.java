package com.ua.estore.cgsWeb.models.dto.roadie;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeliveryWindow {
    private String start;
    private String end;
}
