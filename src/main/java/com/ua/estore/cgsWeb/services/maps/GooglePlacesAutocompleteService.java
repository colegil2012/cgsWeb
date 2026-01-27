package com.ua.estore.cgsWeb.services.maps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.estore.cgsWeb.config.props.GoogleMapsProperties;
import com.ua.estore.cgsWeb.models.dto.AddressDTO;
import com.ua.estore.cgsWeb.models.dto.AddressSuggestion;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class GooglePlacesAutocompleteService {
    private final RestClient restClient;
    private final GoogleMapsProperties props;
    private final ObjectMapper objectMapper;

    public GooglePlacesAutocompleteService(RestClient.Builder builder, GoogleMapsProperties props, ObjectMapper objectMapper) {
        this.restClient = builder.baseUrl("https://maps.googleapis.com").build();
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public List<AddressSuggestion> suggestUsAddresses(String query) {
        if (query == null || query.trim().length() < 3) return List.of();
        if (props.apiKey() == null || props.apiKey().isBlank() || props.apiKey().contains("<")) return List.of();

        Object raw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maps/api/place/autocomplete/json")
                        .queryParam("input", query.trim())
                        .queryParam("types", "address")
                        .queryParam("components", "country:us")
                        .queryParam("key", props.apiKey())
                        .build())
                .retrieve()
                .body(Object.class);

        JsonNode root = objectMapper.valueToTree(raw);
        JsonNode preds = root.path("predictions");

        if (!preds.isArray()) return List.of();

        List<AddressSuggestion> out = new ArrayList<>();
        for (JsonNode p : preds) {
            String label = p.path("description").asText(null);
            String placeId = p.path("place_id").asText(null);

            if (label != null && placeId != null) {
                out.add(AddressSuggestion.basic(label, placeId));
            }
        }
        return out;
    }

    public AddressDTO resolveUsAddress(String placeId) {
        if (placeId == null || placeId.isBlank()) return null;
        if (props.apiKey() == null || props.apiKey().isBlank() || props.apiKey().contains("<")) return null;

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
        JsonNode result = root.path("result");
        JsonNode components = result.path("address_components");
        if (!components.isArray()) return null;

        String streetNumber = null;
        String route = null;
        String locality = null;
        String adminArea1 = null; // state
        String postalCode = null;

        for (JsonNode c : components) {
            JsonNode types = c.path("types");
            if (!types.isArray()) continue;

            if (hasType(types, "street_number")) streetNumber = c.path("long_name").asText(null);
            if (hasType(types, "route")) route = c.path("long_name").asText(null);

            // "locality" is typical US city. Some places may use "postal_town".
            if (hasType(types, "locality") && locality == null) locality = c.path("long_name").asText(null);
            if (hasType(types, "postal_town") && locality == null) locality = c.path("long_name").asText(null);

            if (hasType(types, "administrative_area_level_1")) adminArea1 = c.path("short_name").asText(null);
            if (hasType(types, "postal_code")) postalCode = c.path("long_name").asText(null);
        }

        String street = joinNonBlank(" ", streetNumber, route);

        // Return what we can; frontend can still allow manual edits.
        return new AddressDTO(street, locality, adminArea1, postalCode);
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
}
