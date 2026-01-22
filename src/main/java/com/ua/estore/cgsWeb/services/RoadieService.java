package com.ua.estore.cgsWeb.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.estore.cgsWeb.models.dto.RoadieEstimateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoadieService {

    @Value("${roadie.api.key}")
    private String apiKey;

    @Value("${roadie.url}")
    private String baseUrl;

    @Value("${roadie.api.version}")
    private String apiVersion;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> getEstimate(RoadieEstimateRequest request) {
        String url = String.format("%s/%s/estimates", baseUrl, apiVersion);

        try {
            // Log the outgoing request
            System.out.println("--- ROADIE API REQUEST ---");
            System.out.println("URL: " + url);
            System.out.println("Payload: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<RoadieEstimateRequest> entity = new HttpEntity<>(request, headers);

            var response = restTemplate.postForObject(url, entity, Map.class);
            System.out.println("--- ROADIE API RESPONSE ---");
            System.out.println("Response: " + response);
            return response;

        } catch (Exception e) {
            System.err.println("Failed to call Roadie API: " + e.getMessage());
            return Map.of("error", "Failed to get Estimate: " + e.getMessage());
        }
    }
}
