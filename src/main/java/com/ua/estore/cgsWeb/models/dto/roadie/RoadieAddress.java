package com.ua.estore.cgsWeb.models.dto.roadie;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoadieAddress {
    private String street1;
    private String city;
    private String state;
    private String zip;
}
