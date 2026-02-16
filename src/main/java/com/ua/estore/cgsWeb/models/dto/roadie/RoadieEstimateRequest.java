package com.ua.estore.cgsWeb.models.dto.roadie;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ua.estore.cgsWeb.models.Address;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoadieEstimateRequest {

    @JsonProperty("pickup_location")
    private EstimateLocation pickupLocation;

    @JsonProperty("delivery_location")
    private EstimateLocation deliveryLocation;

    @JsonProperty("pickup_after")
    private String pickupAfter;

    @JsonProperty("deliver_between")
    private DeliveryWindow deliverBetween;

    private List<Package> items;
}
