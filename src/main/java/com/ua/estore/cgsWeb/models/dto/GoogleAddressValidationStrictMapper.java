package com.ua.estore.cgsWeb.models.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class GoogleAddressValidationStrictMapper {
    public static ValidatedAddress mapHighCertainty(JsonNode root) {
        // Defensive defaults
        if (root == null || root.isNull()) {
            return new ValidatedAddress(false, 0, 0, null, "No response from address validation service.");
        }

        JsonNode result = root.path("result");
        JsonNode verdict = result.path("verdict");

        boolean addressComplete = verdict.path("addressComplete").asBoolean(false);
        boolean hasUnconfirmed = verdict.path("hasUnconfirmedComponents").asBoolean(true);
        boolean hasInferred = verdict.path("hasInferredComponents").asBoolean(true);

        String validationGranularity = verdict.path("validationGranularity").asText("");
        // We’re being strict: require a premise-level validation.
        boolean premiseLevel =
                "PREMISE".equalsIgnoreCase(validationGranularity)
                        || "SUB_PREMISE".equalsIgnoreCase(validationGranularity);

        // Location
        JsonNode geocode = result.path("geocode");
        JsonNode location = geocode.path("location");
        boolean hasLatLng = location.hasNonNull("latitude") && location.hasNonNull("longitude");

        double lat = hasLatLng ? location.path("latitude").asDouble() : 0;
        double lng = hasLatLng ? location.path("longitude").asDouble() : 0;

        String geocodeGranularity = geocode.path("geocodeGranularity").asText("");
        boolean geocodePremiseLevel =
                "PREMISE".equalsIgnoreCase(geocodeGranularity)
                        || "SUB_PREMISE".equalsIgnoreCase(geocodeGranularity);

        // Formatted address (nice for logs / display)
        String formatted = result.path("address").path("formattedAddress").asText(null);

        if (!addressComplete) {
            return new ValidatedAddress(false, 0, 0, formatted, "Address is incomplete. Please include street number, city, state, and ZIP.");
        }
        if (hasUnconfirmed) {
            return new ValidatedAddress(false, 0, 0, formatted, "Address could not be confirmed with high certainty.");
        }
        if (hasInferred) {
            return new ValidatedAddress(false, 0, 0, formatted, "Address required inferred components; please enter the full address exactly.");
        }
        if (!premiseLevel) {
            return new ValidatedAddress(false, 0, 0, formatted, "Address is not specific enough (must be a full street address).");
        }
        if (!hasLatLng || !geocodePremiseLevel) {
            return new ValidatedAddress(false, 0, 0, formatted, "Address geocode was not precise enough for service-area validation.");
        }

        return new ValidatedAddress(true, lat, lng, formatted, null);
    }

    private GoogleAddressValidationStrictMapper() {}
}
