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

        Map<String, Object> address = new LinkedHashMap<>();
        address.put("regionCode", "US");
        address.put("addressLines", new String[]{
                input.street(),
                input.city() + ", " + input.state() + " " + input.zip()
        });

        Map<String, Object> body = Map.of("address", address);

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
    }
}
