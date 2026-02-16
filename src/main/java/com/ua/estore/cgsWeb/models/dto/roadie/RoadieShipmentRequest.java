package com.ua.estore.cgsWeb.models.dto.roadie;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ua.estore.cgsWeb.models.Address;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoadieShipmentRequest {

    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    @JsonProperty("alternate_id_1")
    private String altId1;

    @JsonProperty("alternate_id_2")
    private String altId2;

    @JsonProperty("description")
    private String description;

    @JsonProperty("pickup_location")
    private ShipmentLocation pickupLocation;

    @JsonProperty("delivery_location")
    private ShipmentLocation deliveryLocation;

    @JsonProperty("pickup_after")
    private String pickupAfter;

    @JsonProperty("deliver_between")
    private DeliveryWindow deliverBetween;

    @JsonProperty("items")
    private List<Package> items;

    @JsonProperty("options")
    private List<Options> options;
}
