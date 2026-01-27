package com.ua.estore.cgsWeb.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoadieEstimateRequest {

    @JsonProperty("pickup_location")
    private Location pickupLocation;

    @JsonProperty("delivery_location")
    private Location deliveryLocation;

    @JsonProperty("pickup_after")
    private String pickupAfter;

    @JsonProperty("deliver_between")
    private DeliveryWindow deliverBetween;

    private List<Package> items;

    @Data
    @Builder
    public static class Location {
        private Address address;
    }

    @Data
    @Builder
    public static class Address {
        private String street1;
        private String street2;
        private String city;
        private String state;
        private String zip;
    }

    @Data
    @Builder
    public static class DeliveryWindow {
        private String start;
        private String end;
    }

    @Data
    @Builder
    public static class Package {
        private String length;
        private String width;
        private String height;
        private String weight;
        private int quantity;
    }
}
