package com.ua.estore.cgsWeb.models.dto.roadie;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipmentLocation {
    private RoadieAddress address;
    private Contact contact;
    private String notes;
}
