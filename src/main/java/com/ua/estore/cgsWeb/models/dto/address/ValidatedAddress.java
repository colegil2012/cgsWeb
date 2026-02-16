package com.ua.estore.cgsWeb.models.dto.address;

public record ValidatedAddress(boolean valid, double lat, double lng, String formattedAddress, String message) {}
