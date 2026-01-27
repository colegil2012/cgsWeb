package com.ua.estore.cgsWeb.models.dto;

public record AddressSuggestion(
        String label,
        String placeId,
        String street,
        String city,
        String state,
        String zip
) {
    public static AddressSuggestion basic(String label, String placeId) {
        return new AddressSuggestion(label, placeId, null, null, null, null);
    }

    public boolean hasComponents() {
        return street != null && city != null && state != null && zip != null;
    }
}
