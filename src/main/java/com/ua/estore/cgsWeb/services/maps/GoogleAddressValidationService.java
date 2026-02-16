package com.ua.estore.cgsWeb.services.maps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.estore.cgsWeb.config.props.GoogleMapsProperties;
import com.ua.estore.cgsWeb.models.dto.address.AddressDTO;
import com.ua.estore.cgsWeb.models.dto.address.GoogleAddressValidationStrictMapper;
import com.ua.estore.cgsWeb.models.dto.address.ValidatedAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
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
                || isBlank(input.street1())
                || isBlank(input.city())
                || isBlank(input.state())
                || isBlank(input.zip())) {
            return new ValidatedAddress(false, 0, 0, null, "Address is incomplete (street/city/state/zip required).");
        }

        Map<String, Object> address = new LinkedHashMap<>();
        address.put("regionCode", "US");
        address.put("addressLines", new String[]{
                input.street1(),
                input.city() + ", " + input.state() + " " + input.zip()
        });

        Map<String, Object> body = Map.of("address", address);
        // Never log API keys; log a "safe" URL without key
        final String safeUrl = "/v1:validateAddress?key=<redacted>";

        long startNanos = System.nanoTime();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Google Address Validation request -> url={}, body={}",
                        safeUrl, toJsonSafe(body, 1200));
            } else {
                // Keep INFO concise; you can flip to DEBUG when troubleshooting.
                log.info("Google Address Validation request -> url={}, street='{}', city='{}', state='{}', zip='{}'",
                        safeUrl, input.street1(), input.city(), input.state(), input.zip());
            }

            Object raw = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1:validateAddress")
                            .queryParam("key", props.apiKey())
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Object.class);

            long tookMs = (System.nanoTime() - startNanos) / 1_000_000;

            if (log.isDebugEnabled()) {
                log.debug("Google Address Validation response <- url={}, tookMs={}, body={}",
                        safeUrl, tookMs, toJsonSafe(raw, 2000));
            } else {
                log.info("Google Address Validation response <- url={}, tookMs={}", safeUrl, tookMs);
            }

            JsonNode root = objectMapper.valueToTree(raw);
            return GoogleAddressValidationStrictMapper.mapHighCertainty(root);

        } catch (RestClientResponseException ex) {
            long tookMs = (System.nanoTime() - startNanos) / 1_000_000;

            String responseBody = ex.getResponseBodyAsString();
            log.warn("Google Address Validation failed <- url={}, tookMs={}, status={} {}, responseBody={}",
                    safeUrl,
                    tookMs,
                    ex.getRawStatusCode(),
                    ex.getStatusText(),
                    truncate(responseBody, 2000));

            String msg = "Google Address Validation failed (" + ex.getRawStatusCode() + " " + ex.getStatusText() + ").";

            if (responseBody != null && !responseBody.isBlank()) {
                msg += " Response: " + truncate(responseBody, 600);
            } else {
                msg += " No response body returned by Google.";
            }

            return new ValidatedAddress(false, 0, 0, null, msg);

        } catch (Exception ex) {
            long tookMs = (System.nanoTime() - startNanos) / 1_000_000;

            log.error("Google Address Validation request error <- url={}, tookMs={}, errorType={}, message={}",
                    safeUrl, tookMs, ex.getClass().getSimpleName(), ex.getMessage(), ex);

            return new ValidatedAddress(false, 0, 0, null,
                    "Google Address Validation request failed: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String toJsonSafe(Object value, int maxLen) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
            return truncate(json, maxLen);
        } catch (Exception e) {
            return "<unserializable:" + e.getClass().getSimpleName() + ">";
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...(truncated)";
    }
}
