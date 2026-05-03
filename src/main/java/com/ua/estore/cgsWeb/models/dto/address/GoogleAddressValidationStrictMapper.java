package com.ua.estore.cgsWeb.models.dto.address;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

/**
 * Strict mapper from a Google Address Validation v1 response to {@link ValidatedAddress}.
 *
 * <p>Mirrors the verdict-based contract the service layer expects:
 * <ul>
 *   <li>Reject anything not {@code addressComplete}.</li>
 *   <li>Reject {@code hasUnconfirmedComponents}.</li>
 *   <li>Reject {@code hasInferredComponents} unless Google explicitly recommends ACCEPT
 *       — and when we do reject, the message contains the literal phrase
 *       {@code "inferred components"} so {@code ServiceAreaValidationService} can rewrite
 *       it into a friendlier user-facing string.</li>
 *   <li>Require a premise-level validation AND a premise-level geocode (so we don't
 *       attempt service-area math on an interpolated street midpoint).</li>
 * </ul>
 */

@Slf4j
public final class GoogleAddressValidationStrictMapper {

    private GoogleAddressValidationStrictMapper() {}

    public static ValidatedAddress mapHighCertainty(JsonNode root) {
        if (root == null || root.isNull()) {
            return new ValidatedAddress(false, 0, 0, null,
                    "No response from address validation service.");
        }

        JsonNode result = root.path("result");
        JsonNode verdict = result.path("verdict");

        boolean addressComplete = verdict.path("addressComplete").asBoolean(false);
        boolean hasUnconfirmed  = verdict.path("hasUnconfirmedComponents").asBoolean(false);
        boolean hasInferred     = verdict.path("hasInferredComponents").asBoolean(false);

        String validationGranularity = verdict.path("validationGranularity").asText("");
        String possibleNextAction    = verdict.path("possibleNextAction").asText("");
        String geocodeGranularity    = verdict.path("geocodeGranularity").asText("");

        boolean premiseLevel =
                "PREMISE".equalsIgnoreCase(validationGranularity)
                        || "SUB_PREMISE".equalsIgnoreCase(validationGranularity);

        boolean geocodePremiseLevel =
                "PREMISE".equalsIgnoreCase(geocodeGranularity)
                        || "SUB_PREMISE".equalsIgnoreCase(geocodeGranularity);

        JsonNode geocode  = result.path("geocode");
        JsonNode location = geocode.path("location");
        boolean hasLatLng = location.hasNonNull("latitude") && location.hasNonNull("longitude");

        double lat = hasLatLng ? location.path("latitude").asDouble() : 0;
        double lng = hasLatLng ? location.path("longitude").asDouble() : 0;

        String formatted = result.path("address").path("formattedAddress").asText(null);

        log.info("Address validation verdict: formatted='{}', complete={}, inferred={}, " +
                        "unconfirmed={}, validationGranularity={}, geocodeGranularity={}, " +
                        "hasLatLng={}, nextAction={}",
                formatted, addressComplete, hasInferred, hasUnconfirmed, validationGranularity,
                geocodeGranularity, hasLatLng, possibleNextAction);

        if (!addressComplete) {
            return new ValidatedAddress(false, 0, 0, formatted,
                    "Address is incomplete. Please include street number, city, state, and ZIP.");
        }
        if (hasUnconfirmed) {
            return new ValidatedAddress(false, 0, 0, formatted,
                    "Address could not be confirmed with high certainty.");
        }

        // Google can flag inferred components but still recommend ACCEPT – respect that,
        // and only fail when ACCEPT isn't the next action.
        boolean acceptableDespiteInferred = "ACCEPT".equalsIgnoreCase(possibleNextAction);
        if (hasInferred && !acceptableDespiteInferred) {
            return new ValidatedAddress(false, 0, 0, formatted,
                    "Address required inferred components; please enter the full address exactly.");
        }

        if (!premiseLevel) {
            return new ValidatedAddress(false, 0, 0, formatted,
                    "Address is not specific enough (must be a full street address).");
        }
        if (!hasLatLng || !geocodePremiseLevel) {
            return new ValidatedAddress(false, 0, 0, formatted,
                    "Address geocode was not precise enough for service-area validation.");
        }

        return new ValidatedAddress(true, lat, lng, formatted, null);
    }
}