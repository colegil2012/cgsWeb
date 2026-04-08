package com.ua.estore.cgsWeb.services.shipping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ua.estore.cgsWeb.models.Address;
import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.Vendor;
import com.ua.estore.cgsWeb.models.dto.product.ProductDTO;
import com.ua.estore.cgsWeb.models.dto.roadie.*;
import com.ua.estore.cgsWeb.models.dto.roadie.Package;
import com.ua.estore.cgsWeb.models.dto.shop.OrderDTO;
import com.ua.estore.cgsWeb.models.wrappers.PackageWrapper;
import com.ua.estore.cgsWeb.services.shop.ProductService;
import com.ua.estore.cgsWeb.services.vendor.VendorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

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
    private final ProductService productService;
    private final VendorService vendorService;


    /********************************************************************
     * Estimate Shipping Costs with Roadie
     ********************************************************************/

    public Map<String, Object> getShippingEstimates(User user, List<ProductDTO> cartItems,
                                                    String shippingAddressId) {

        Address deliveryAddress = resolveDeliveryAddress(user, shippingAddressId);
        if (deliveryAddress == null) {
            return Map.of("error", "No delivery address found.");
        }

        List<Map<String, Object>> shippingEstimates = new ArrayList<>();
        BigDecimal totalShipping = BigDecimal.ZERO;

        if (cartItems.isEmpty()) {
            return Map.of(
                    "shippingEstimates", shippingEstimates,
                    "totalShipping", totalShipping
            );
        }

        Map<String, PackageWrapper> vendorPackages = new HashMap<>();
        for (ProductDTO item : cartItems) {
            Product p = productService.getProductById(item.getId());
            if (p != null) {
                vendorPackages.computeIfAbsent(p.getVendorId(),
                                id -> new PackageWrapper(id, item.getVendorName()))
                        .addItem(item, p);
            }
        }

        for (PackageWrapper pkg : vendorPackages.values()) {
            Vendor vendor = vendorService.getVendorById(pkg.getVendorId()).orElse(null);
            if (vendor == null) continue;

            var pickupAddr = vendor.getAddresses().stream()
                    .filter(Address::isDefault).findFirst()
                    .orElse(vendor.getAddresses().isEmpty() ? null : vendor.getAddresses().get(0));

            if (pickupAddr == null) continue;

            int maxProductReadyTime = pkg.getItems().stream()
                    .map(i -> productService.getProductById(i.getId()))
                    .filter(Objects::nonNull)
                    .mapToInt(Product::getReadyTime)
                    .max()
                    .orElse(0);

            int totalLeadTimeDays = vendor.getLead_time() + maxProductReadyTime;

            OffsetDateTime pickupTime = OffsetDateTime.now().plusDays(totalLeadTimeDays);
            OffsetDateTime deliveryDeadline = pickupTime.plusDays(1);

            RoadieEstimateRequest request = RoadieEstimateRequest.builder()
                    .pickupLocation(EstimateLocation.builder()
                            .address(RoadieAddress.builder()
                                    .street1((pickupAddr.getStreet1() + " " + pickupAddr.getStreet2()).trim())
                                    .city(pickupAddr.getCity())
                                    .state(pickupAddr.getState())
                                    .zip(pickupAddr.getZip()).build())
                            .build())
                    .deliveryLocation(EstimateLocation.builder()
                            .address(RoadieAddress.builder()
                                    .street1((deliveryAddress.getStreet1() + " " + deliveryAddress.getStreet2()).trim())
                                    .city(deliveryAddress.getCity())
                                    .state(deliveryAddress.getState())
                                    .zip(deliveryAddress.getZip()).build())
                            .build())
                    .pickupAfter(pickupTime.toString())
                    .deliverBetween(DeliveryWindow.builder()
                            .start(deliveryDeadline.toString())
                            .end(deliveryDeadline.plusHours(2).toString())
                            .build())
                    .items(List.of(Package.builder()
                            .length(String.valueOf(pkg.getTotalLength()))
                            .width(String.valueOf(pkg.getTotalWidth()))
                            .height(String.valueOf(pkg.getTotalHeight()))
                            .weight(String.valueOf(pkg.getTotalWeight()))
                            .quantity(1).build()))
                    .build();

            Map<String, Object> response;

            String url = String.format("%s/%s/estimates", baseUrl, apiVersion);

            try {
                normalizeZip5InPlace(request);

                log.info("Estimate Request Payload: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);
                HttpEntity<RoadieEstimateRequest> entity = new HttpEntity<>(request, headers);

                response = restTemplate.postForObject(url, entity, Map.class);
                log.info("Estimate Request Response: " + response);
            } catch (Exception e) {
                log.error("Failed to call Roadie API: " + e.getMessage());
                return Map.of("error", "Failed to get Estimate: " + e.getMessage());
            }

            if (response != null && !response.containsKey("error")) {
                BigDecimal price = extractPrice(response);
                if (price != null) {
                    totalShipping = totalShipping.add(price);
                    shippingEstimates.add(Map.of("vendor", pkg.getVendorName(), "cost", price));
                }
            } else {
                log.warn("Estimate failed for vendor {}: {}", pkg.getVendorName(),
                        response != null ? response.get("error") : "null response");
            }
        }

        return Map.of(
                "shippingEstimates", shippingEstimates,
                "totalShipping", totalShipping
        );
    }

    /********************************************************************
     * Create Shipment with Roadie
     ********************************************************************/

     public Map<String, Object> createShipment(OrderDTO orderList, String shippingAddressId) {

         Address deliveryAddr = resolveDeliveryAddress(orderList.getUser(), shippingAddressId);
         if (deliveryAddr == null) {
             return Map.of("error", "No delivery address found.");
         }

         List<ProductDTO> cartItems = orderList.getProducts();
         List<Map<String, Object>> shipments = new ArrayList<>();

         if (cartItems.isEmpty()) {
             return Map.of(
                     "shipments", shipments
             );
         }

         Map<String, PackageWrapper> vendorPackages = new HashMap<>();
         for (ProductDTO item : cartItems) {
             Product p = productService.getProductById(item.getId());
             if (p != null) {
                 vendorPackages.computeIfAbsent(p.getVendorId(),
                                 id -> new PackageWrapper(id, item.getVendorName()))
                         .addItem(item, p);
             }
         }

         for (PackageWrapper pkg : vendorPackages.values()) {
             Vendor vendor = vendorService.getVendorById(pkg.getVendorId()).orElse(null);
             if (vendor == null) continue;

             var pickupAddr = vendor.getAddresses().stream()
                     .filter(Address::isDefault).findFirst()
                     .orElse(vendor.getAddresses().isEmpty() ? null : vendor.getAddresses().get(0));

             if (pickupAddr == null) continue;

             int maxProductReadyTime = pkg.getItems().stream()
                     .map(i -> productService.getProductById(i.getId()))
                     .filter(Objects::nonNull)
                     .mapToInt(Product::getReadyTime)
                     .max()
                     .orElse(0);

             int totalLeadTimeDays = vendor.getLead_time() + maxProductReadyTime;

             OffsetDateTime pickupTime = OffsetDateTime.now().plusDays(totalLeadTimeDays);
             OffsetDateTime deliveryDeadline = pickupTime.plusDays(1);

             RoadieShipmentRequest request = RoadieShipmentRequest.builder()
                     .referenceId(orderList.getOrderId())
                     .idempotencyKey(String.valueOf(orderList.getIdempotencyKey()))
                     .description(String.format("Order: %s, User: %s, Items: %s",
                             orderList.getOrderId(), orderList.getUser().getUsername(), orderList.getProducts().size()))
                     .pickupLocation(ShipmentLocation.builder()
                             .address(RoadieAddress.builder()
                                     .street1((pickupAddr.getStreet1() + " " + pickupAddr.getStreet2()).trim())
                                     .city(pickupAddr.getCity())
                                     .state(pickupAddr.getState())
                                     .zip(pickupAddr.getZip())
                                     .build())
                             .build())
                     .deliveryLocation(ShipmentLocation.builder()
                             .address(RoadieAddress.builder()
                                     .street1(deliveryAddr.getStreet1())
                                     .city(deliveryAddr.getCity())
                                     .state(deliveryAddr.getState())
                                     .zip(deliveryAddr.getZip())
                                     .build())
                             .build())
                     .pickupAfter(pickupTime.toString())
                     .deliverBetween(DeliveryWindow.builder()
                             .start(deliveryDeadline.toString())
                             .end(deliveryDeadline.plusHours(2).toString())
                             .build())
                     .items(List.of(Package.builder()
                             .length(String.valueOf(pkg.getTotalLength()))
                             .width(String.valueOf(pkg.getTotalWidth()))
                             .height(String.valueOf(pkg.getTotalHeight()))
                             .weight(String.valueOf(pkg.getTotalWeight()))
                             .quantity(1).build()))
                     .build();

             Map<String, Object> response;

             String url = String.format("%s/%s/shipments", baseUrl, apiVersion);

             try {
                 log.info("Shipment Request Payload: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));

                 HttpHeaders headers = new HttpHeaders();
                 headers.setContentType(MediaType.APPLICATION_JSON);
                 headers.setBearerAuth(apiKey);
                 HttpEntity<RoadieShipmentRequest> entity = new HttpEntity<>(request, headers);

                 response = restTemplate.postForObject(url, entity, Map.class);
                 log.info("Shipment Request Response: " + response);

             } catch (Exception e) {
                 log.error("Failed to create Roadie Shipment: " + e.getMessage());
                 return Map.of("error", "Failed to create shipment: " + e.getMessage());
             }

             if (response != null && !response.containsKey("error")) {
                 if (response.containsKey("id") && response.containsKey("tracking_number")) {
                     shipments.add(Map.of("orderId", response.get("id"),
                             "tracking", response.get("tracking_number")));
                 }
             } else {
                 log.warn("Estimate failed for vendor {}: {}", pkg.getVendorName(),
                         response != null ? response.get("error") : "null response");
             }
         }

         return Map.of(
                 "shipments", shipments
         );
    }


    /******************************************************
     * HelperMethods
     ******************************************************/

    public Address resolveDeliveryAddress(User user, String shippingAddressId) {
        if (user.getAddresses() == null || user.getAddresses().isEmpty()) {
            return null;
        }
        Address deliveryAddress = null;
        if (shippingAddressId != null && !shippingAddressId.isBlank()) {
            deliveryAddress = user.getAddresses().stream()
                    .filter(a -> a != null && a.getAddressId() != null && a.getAddressId().equals(shippingAddressId))
                    .findFirst()
                    .orElse(null);
        }
        if (deliveryAddress == null) {
            deliveryAddress = user.getAddresses().stream()
                    .filter(Address::isDefault)
                    .findFirst()
                    .orElse(user.getAddresses().get(0));
        }
        return deliveryAddress;
    }

    private BigDecimal extractPrice(Map<String, Object> response) {
        if (response == null) return null;

        Object costObj = null;
        boolean isCents = false;

        if (response.containsKey("price")) {
            costObj = response.get("price");
        } else if (response.containsKey("estimated_amount")) {
            costObj = response.get("estimated_amount");
            isCents = true;
        }

        if (costObj == null) return null;

        BigDecimal price = new BigDecimal(costObj.toString());
        if (isCents) {
            price = price.divide(new BigDecimal(100));
        }
        return price;
    }

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
