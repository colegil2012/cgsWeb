package com.ua.estore.cgsWeb.models.dto.address;

public record AddressSuggestion(
        String label,
        String placeId,
        String street1 ,
        String street2,
        String city,
        String state,
        String zip
) {
    public static AddressSuggestion basic(String label, String placeId) {
        return new AddressSuggestion(label, placeId, null, null, null, null, null);
    }

    public boolean hasComponents() {
        return street1 != null && city != null && state != null && zip != null;
    }
}
