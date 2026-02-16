package com.ua.estore.cgsWeb.services.shipping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.estore.cgsWeb.models.dto.roadie.EstimateLocation;
import com.ua.estore.cgsWeb.models.dto.roadie.RoadieEstimateRequest;
import com.ua.estore.cgsWeb.models.dto.roadie.RoadieShipmentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
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


    /********************************************************************
     * Estimate Shipping Costs with Roadie
     ********************************************************************/

    public Map<String, Object> getEstimate(RoadieEstimateRequest request) {
        String url = String.format("%s/%s/estimates", baseUrl, apiVersion);

        try {
            normalizeZip5InPlace(request);

            log.info("Estimate Request Payload: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<RoadieEstimateRequest> entity = new HttpEntity<>(request, headers);

            var response = restTemplate.postForObject(url, entity, Map.class);
            log.info("Estimate Request Response: " + response);
            return response;

        } catch (Exception e) {
            log.error("Failed to call Roadie API: " + e.getMessage());
            return Map.of("error", "Failed to get Estimate: " + e.getMessage());
        }
    }

    /********************************************************************
     * Create Shipment with Roadie
     ********************************************************************/

    public Map<String, Object> createShipment(RoadieShipmentRequest request) {
        String url = String.format("%s/%s/shipments", baseUrl, apiVersion);

        try {
            log.info("Shipment Request Payload: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<RoadieShipmentRequest> entity = new HttpEntity<>(request, headers);

            var response = restTemplate.postForObject(url, entity, Map.class);
            log.info("Shipment Request Response: " + response);
            return response;

        } catch (Exception e) {
            log.error("Failed to create Roadie Shipment: " + e.getMessage());
            return Map.of("error", "Failed to get Estimate: " + e.getMessage());
        }
    }


    /******************************************************
     * HelperMethods
     ******************************************************/

    private static void normalizeZip5InPlace(RoadieEstimateRequest request) {
        if (request == null) return;

        normalizeLocationZip(request.getPickupLocation());
        normalizeLocationZip(request.getDeliveryLocation());
    }


    private static void normalizeLocationZip(EstimateLocation loc) {
        if (loc == null || loc.getAddress() == null) return;
        var addr = loc.getAddress();
        addr.setZip(zip5OrNull(addr.getZip()));
    }

    private static String zip5OrNull(String zip) {
        if (zip == null) return null;
        String z = zip.trim();
        if (z.length() >= 5 && z.substring(0, 5).matches("^\\d{5}$")) return z.substring(0, 5);
        return z; // leave it as-is if it’s already weird; Roadie will error, but logs will show the bad value
    }

}
