package com.ua.estore.cgsWeb.controllers.shipping;

import com.ua.estore.cgsWeb.models.address.Address;
import com.ua.estore.cgsWeb.models.shop.Cart;
import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.models.shipping.ShippingEstimate;
import com.ua.estore.cgsWeb.services.shipping.ShippingEstimateService;
import com.ua.estore.cgsWeb.services.shop.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only JSON endpoint used by cart.js / checkout.js to refresh shipping
 * estimates when the customer changes delivery address without a full page reload.
 */
@Slf4j
@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingApiController {

    private final CartService cartService;
    private final ShippingEstimateService shippingEstimateService;

    @GetMapping("/estimate")
    public ResponseEntity<?> estimate(@RequestParam String addressId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Address destination = Optional.ofNullable(user.getAddresses()).orElse(List.of()).stream()
                .filter(Objects::nonNull)
                .filter(a -> Objects.equals(a.getAddressId(), addressId))
                .findFirst()
                .orElse(null);

        if (destination == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown addressId"));
        }

        Cart cart = cartService.getOrCreateByUserId(user.getId());
        List<ShippingEstimate> estimates = shippingEstimateService.estimateForCart(cart, destination);

        BigDecimal totalShipping = estimates.stream()
                .map(ShippingEstimate::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
                "estimates", estimates,
                "totalShipping", totalShipping
        ));
    }
}