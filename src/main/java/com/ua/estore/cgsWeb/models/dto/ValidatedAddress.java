package com.ua.estore.cgsWeb.models.dto;

public record ValidatedAddress(boolean valid, double lat, double lng, String formattedAddress, String message) {}
