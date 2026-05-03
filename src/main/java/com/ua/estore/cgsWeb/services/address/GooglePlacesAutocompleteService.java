package com.ua.estore.cgsWeb.services.address;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.estore.cgsWeb.config.props.GoogleMapsProperties;
import com.ua.estore.cgsWeb.models.dto.address.AddressDTO;
import com.ua.estore.cgsWeb.models.dto.address.AddressSuggestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Client for Google's (legacy) Places Autocomplete + Place Details endpoints.
 *
 * <p>Two-step flow:</p>
 * <ol>
 *   <li>{@link #suggestUsAddresses(String)} – returns lightweight {@link AddressSuggestion}s
 *       containing only {@code label} + {@code placeId}. Google does not return parsed
 *       components from autocomplete, so the JS must call /resolve when the user clicks
 *       a suggestion (this is the opposite of how MapTiler worked).</li>
 *   <li>{@link #resolveUsAddress(String)} – follows up with Place Details to extract
 *       street_number, route, locality, administrative_area_level_1 (state code),
 *       and postal_code into an {@link AddressDTO} the form can autofill.</li>
 * </ol>
 *
 * <p>If Phase-4 debugging reveals the GCP project only has Places API (New) enabled,
 * the swap is contained to this one class – the controller and JS contracts stay stable.</p>
 */
@Slf4j
@Service
public class GooglePlacesAutocompleteService {

    private final RestClient restClient;
    private final GoogleMapsProperties props;
    private final ObjectMapper objectMapper;

    public GooglePlacesAutocompleteService(RestClient.Builder builder,
                                           GoogleMapsProperties props,
                                           ObjectMapper objectMapper) {
        this.restClient = builder.baseUrl("https://maps.googleapis.com").build();
        this.props = props;
        this.objectMapper = objectMapper;
    }

    /* =========================================================================================
     * Autocomplete
     * =======================================================================================*/

    public List<AddressSuggestion> suggestUsAddresses(String query) {
        if (query == null || query.trim().length() < 3) return List.of();
        if (!apiKeyConfigured()) return List.of();

        String q = query.trim();
        try {
            Object raw = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/maps/api/place/autocomplete/json")
                            .queryParam("input", q)
                            .queryParam("types", "address")
                            .queryParam("components", "country:us")
                            .queryParam("key", props.apiKey())
                            .build())
                    .retrieve()
                    .body(Object.class);

            JsonNode root = objectMapper.valueToTree(raw);

            // Surface Google's status when it isn't OK – this is the single biggest source
            // of "no suggestions" symptoms (REQUEST_DENIED, ZERO_RESULTS, OVER_QUERY_LIMIT).
            String status = root.path("status").asText("");
            if (!"OK".equalsIgnoreCase(status) && !"ZERO_RESULTS".equalsIgnoreCase(status)) {
                log.warn("Google Places autocomplete non-OK status='{}', error_message='{}'",
                        status, root.path("error_message").asText(""));
                return List.of();
            }

            JsonNode preds = root.path("predictions");
            if (!preds.isArray()) return List.of();

            List<AddressSuggestion> out = new ArrayList<>(preds.size());
            for (JsonNode p : preds) {
                String label   = p.path("description").asText(null);
                String placeId = p.path("place_id").asText(null);
                if (label != null && placeId != null) {
                    out.add(AddressSuggestion.basic(label, placeId));
                }
            }
            return out;

        } catch (RestClientResponseException ex) {
            log.warn("Google Places autocomplete failed: status={} {}, body={}",
                    ex.getRawStatusCode(), ex.getStatusText(),
                    truncate(ex.getResponseBodyAsString(), 600));
            return List.of();
        } catch (Exception ex) {
            log.error("Google Places autocomplete error: {}: {}",
                    ex.getClass().getSimpleName(), ex.getMessage());
            return List.of();
        }
    }

    /* =========================================================================================
     * Place details (resolve)
     * =======================================================================================*/

    public AddressDTO resolveUsAddress(String placeId) {
        if (placeId == null || placeId.isBlank()) return null;
        if (!apiKeyConfigured()) return null;

        try {
            Object raw = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/maps/api/place/details/json")
                            .queryParam("place_id", placeId)
                            .queryParam("fields", "address_component")
                            .queryParam("key", props.apiKey())
                            .build())
                    .retrieve()
                    .body(Object.class);

            JsonNode root = objectMapper.valueToTree(raw);

            String status = root.path("status").asText("");
            if (!"OK".equalsIgnoreCase(status)) {
                log.warn("Google Places details non-OK status='{}', error_message='{}'",
                        status, root.path("error_message").asText(""));
                return null;
            }

            JsonNode result = root.path("result");
            JsonNode components = result.path("address_components");
            if (!components.isArray()) return null;

            String streetNumber = null;
            String route        = null;
            String locality     = null;
            String adminArea1   = null;  // state
            String postalCode   = null;

            for (JsonNode c : components) {
                JsonNode types = c.path("types");
                if (!types.isArray()) continue;

                if (hasType(types, "street_number")) streetNumber = c.path("long_name").asText(null);
                if (hasType(types, "route"))         route        = c.path("long_name").asText(null);

                // "locality" is the typical US city; "postal_town" is a UK fallback we keep
                // for safety even though we restrict to country:us in the autocomplete call.
                if (hasType(types, "locality")    && locality == null) locality = c.path("long_name").asText(null);
                if (hasType(types, "postal_town") && locality == null) locality = c.path("long_name").asText(null);

                if (hasType(types, "administrative_area_level_1")) adminArea1 = c.path("short_name").asText(null);
                if (hasType(types, "postal_code"))                 postalCode = c.path("long_name").asText(null);
            }

            String street1 = joinNonBlank(" ", streetNumber, route);
            return new AddressDTO(street1, "", locality, adminArea1, postalCode);

        } catch (RestClientResponseException ex) {
            log.warn("Google Places details failed for placeId='{}': status={} {}, body={}",
                    placeId, ex.getRawStatusCode(), ex.getStatusText(),
                    truncate(ex.getResponseBodyAsString(), 600));
            return null;
        } catch (Exception ex) {
            log.error("Google Places details error for placeId='{}': {}: {}",
                    placeId, ex.getClass().getSimpleName(), ex.getMessage());
            return null;
        }
    }

    /* ---------- helpers ---------- */

    private boolean apiKeyConfigured() {
        String k = props.apiKey();
        if (k == null || k.isBlank() || k.contains("<")) {
            log.warn("Google Maps API key is not configured.");
            return false;
        }
        return true;
    }

    private static boolean hasType(JsonNode typesArray, String type) {
        for (JsonNode t : typesArray) {
            if (type.equals(t.asText())) return true;
        }
        return false;
    }

    private static String joinNonBlank(String sep, String a, String b) {
        boolean aOk = a != null && !a.isBlank();
        boolean bOk = b != null && !b.isBlank();
        if (aOk && bOk) return a.trim() + sep + b.trim();
        if (aOk) return a.trim();
        if (bOk) return b.trim();
        return null;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...(truncated)";
    }
}