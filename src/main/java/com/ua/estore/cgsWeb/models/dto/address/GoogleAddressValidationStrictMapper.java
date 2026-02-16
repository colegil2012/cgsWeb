package com.ua.estore.cgsWeb.models.dto.address;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GoogleAddressValidationStrictMapper {
    public static ValidatedAddress mapHighCertainty(JsonNode root) {
        // Defensive defaults
        if (root == null || root.isNull()) {
            return new ValidatedAddress(false, 0, 0, null, "No response from address validation service.");
        }

        JsonNode result = root.path("result");
        JsonNode verdict = result.path("verdict");

        boolean addressComplete = verdict.path("addressComplete").asBoolean(false);
        boolean hasUnconfirmed = verdict.path("hasUnconfirmedComponents").asBoolean(false);
        boolean hasInferred = verdict.path("hasInferredComponents").asBoolean(false);

        String validationGranularity = verdict.path("validationGranularity").asText("");
        String possibleNextAction = verdict.path("possibleNextAction").asText("");

        String geocodeGranularity = verdict.path("geocodeGranularity").asText("");
        boolean geocodePremiseLevel =
                "PREMISE".equalsIgnoreCase(geocodeGranularity)
                        || "SUB_PREMISE".equalsIgnoreCase(geocodeGranularity);

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

        // Formatted address (nice for logs / display)
        String formatted = result.path("address").path("formattedAddress").asText(null);
        log.info("Address validation verdict: formatted='{}', complete={}, inferred={}, unconfirmed={}, validationGranularity={}, geocodeGranularity={}, hasLatLng={}, nextAction={}",
                formatted, addressComplete, hasInferred, hasUnconfirmed, validationGranularity, geocodeGranularity, hasLatLng, possibleNextAction);

        if (!addressComplete) {
            return new ValidatedAddress(false, 0, 0, formatted, "Address is incomplete. Please include street number, city, state, and ZIP.");
        }
        if (hasUnconfirmed) {
            return new ValidatedAddress(false, 0, 0, formatted, "Address could not be confirmed with high certainty.");
        }

        // Important: Google can set hasInferredComponents=true while still recommending ACCEPT.
        // Treat inferred components as a warning *unless* Google indicates it should not be accepted.
        boolean acceptableDespiteInferred = "ACCEPT".equalsIgnoreCase(possibleNextAction);
        if (hasInferred && !acceptableDespiteInferred) {
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
