package com.ua.estore.cgsWeb.services.maps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.estore.cgsWeb.config.props.GoogleMapsProperties;
import com.ua.estore.cgsWeb.models.dto.AddressDTO;
import com.ua.estore.cgsWeb.models.dto.GoogleAddressValidationStrictMapper;
import com.ua.estore.cgsWeb.models.dto.ValidatedAddress;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class GoogleAddressValidationService {

    private final RestClient restClient;
    private final GoogleMapsProperties props;
    private final ObjectMapper objectMapper;

    public GoogleAddressValidationService(RestClient.Builder restClientBuilder,
                                          GoogleMapsProperties props,
                                          ObjectMapper objectMapper) {
        this.restClient = restClientBuilder
                .baseUrl("https://addressvalidation.googleapis.com")
                .build();
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public ValidatedAddress validateUsHighCertainty(AddressDTO input) {
        if (props.apiKey() == null || props.apiKey().isBlank() || props.apiKey().contains("<")) {
            return new ValidatedAddress(false, 0, 0, null, "Google Maps API key is not configured.");
        }

        if (input == null
                || isBlank(input.street())
                || isBlank(input.city())
                || isBlank(input.state())
                || isBlank(input.zip())) {
            return new ValidatedAddress(false, 0, 0, null, "Address is incomplete (street/city/state/zip required).");
        }

        Map<String, Object> address = new LinkedHashMap<>();
        address.put("regionCode", "US");
        address.put("addressLines", new String[]{
                input.street(),
                input.city() + ", " + input.state() + " " + input.zip()
        });

        Map<String, Object> body = Map.of("address", address);

        try {
            Object raw = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1:validateAddress")
                            .queryParam("key", props.apiKey())
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Object.class);

            JsonNode root = objectMapper.valueToTree(raw);
            return GoogleAddressValidationStrictMapper.mapHighCertainty(root);

        } catch (RestClientResponseException ex) {
            // Handles 4xx/5xx from Google (including your 403 SERVICE_DISABLED)
            String responseBody = ex.getResponseBodyAsString();
            String msg = "Google Address Validation failed (" + ex.getRawStatusCode() + " " + ex.getStatusText() + ").";

            if (responseBody != null && !responseBody.isBlank()) {
                msg += " Response: " + truncate(responseBody, 600);
            } else {
                msg += " No response body returned by Google.";
            }

            return new ValidatedAddress(false, 0, 0, null, msg);

        } catch (Exception ex) {
            // Network/DNS/timeouts/serialization, etc.
            return new ValidatedAddress(false, 0, 0, null,
                    "Google Address Validation request failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...(truncated)";
    }
}
