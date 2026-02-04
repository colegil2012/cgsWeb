package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.Cart;
import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.Vendor;
import com.ua.estore.cgsWeb.models.dto.ProductDTO;
import com.ua.estore.cgsWeb.models.dto.RoadieEstimateRequest;
import com.ua.estore.cgsWeb.models.wrappers.PackageWrapper;
import com.ua.estore.cgsWeb.services.CartService;
import com.ua.estore.cgsWeb.services.CategoryService;
import com.ua.estore.cgsWeb.services.ProductService;
import com.ua.estore.cgsWeb.services.VendorService;
import com.ua.estore.cgsWeb.services.shipping.RoadieService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Controller
@AllArgsConstructor
public class RoadieController {

    private final ProductService productService;
    private final VendorService vendorService;
    private final CategoryService categoryService;
    private final RoadieService roadieService;
    private final CartService cartService;

    /********************************************************************************
     * Endpoint for Roadie shipping calculations
     * Called from cart-update.js
     * Returns Response body to update Cart values
     *******************************************************************************/

    @PostMapping("/api/roadie/estimate")
    @ResponseBody
    public ResponseEntity<?> getShippingEstimate(HttpSession session,
                                                 @RequestParam("shippingAddressId") String shippingAddressId) {
        User user = (User) session.getAttribute("user");
        if(user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        //Get Cart and map to ProductDTOs
        var cartEntity = cartService.getOrCreateByUserId(user.getId());
        List<ProductDTO> cartItems = cartService.mapToProductDTOs(cartEntity, productService, vendorService, categoryService);

        User.Address deliveryAddress = null;
        if (user.getAddresses() != null && !user.getAddresses().isEmpty()) {
            if (shippingAddressId != null && !shippingAddressId.isBlank()) {
                deliveryAddress = user.getAddresses().stream()
                        .filter(a -> a != null && a.getAddressId() != null && a.getAddressId().equals(shippingAddressId))
                        .findFirst()
                        .orElse(null);
            }
            if (deliveryAddress == null) {
                deliveryAddress = user.getAddresses().stream()
                        .filter(User.Address::isDefault)
                        .findFirst()
                        .orElse(user.getAddresses().get(0));
            }
        }

        if (deliveryAddress == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No delivery address found."));
        }

        // --- Roadie Multi-Vendor Shipping Logic using PackageWrapper ---
        List<Map<String, Object>> shippingEstimates = new ArrayList<>();
        BigDecimal totalShipping = BigDecimal.ZERO;

        if (!cartItems.isEmpty()) {
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
                        .filter(Vendor.Address::isDefault).findFirst()
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
                        .pickupLocation(RoadieEstimateRequest.Location.builder()
                                .address(RoadieEstimateRequest.Address.builder()
                                        .street1(pickupAddr.getStreet())
                                        .city(pickupAddr.getCity())
                                        .state(pickupAddr.getState())
                                        .zip(pickupAddr.getZip()).build())
                                .build())
                        .deliveryLocation(RoadieEstimateRequest.Location.builder()
                                .address(RoadieEstimateRequest.Address.builder()
                                        .street1(deliveryAddress.getStreet())
                                        .city(deliveryAddress.getCity())
                                        .state(deliveryAddress.getState())
                                        .zip(deliveryAddress.getZip()).build())
                                .build())
                        .pickupAfter(pickupTime.toString())
                        .deliverBetween(RoadieEstimateRequest.DeliveryWindow.builder()
                                .start(deliveryDeadline.toString())
                                .end(deliveryDeadline.plusHours(2).toString())
                                .build())
                        .items(List.of(RoadieEstimateRequest.Package.builder()
                                .length(String.valueOf(pkg.getTotalLength()))
                                .width(String.valueOf(pkg.getTotalWidth()))
                                .height(String.valueOf(pkg.getTotalHeight()))
                                .weight(String.valueOf(pkg.getTotalWeight()))
                                .quantity(1).build()))
                        .build();

                var response = roadieService.getEstimate(request);

                if (response != null && (response.containsKey("price") || response.containsKey("estimated_amount"))) {
                    Object costObj = response.get("price") != null ? response.get("price") : response.get("estimated_amount");
                    BigDecimal price = new BigDecimal(costObj.toString());

                    if (response.containsKey("estimated_amount")) {
                        price = price.divide(new BigDecimal(100));
                    }

                    totalShipping = totalShipping.add(price);
                    shippingEstimates.add(Map.of("vendor", pkg.getVendorName(), "cost", price));
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "shippingEstimates", shippingEstimates,
                "totalShipping", totalShipping
        ));
    }
}
