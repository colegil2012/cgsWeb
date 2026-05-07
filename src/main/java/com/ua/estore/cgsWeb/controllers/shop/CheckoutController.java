package com.ua.estore.cgsWeb.controllers.shop;

import com.ua.estore.cgsWeb.models.address.Address;
import com.ua.estore.cgsWeb.models.dto.product.ProductDTO;
import com.ua.estore.cgsWeb.models.dto.shop.OrderDTO;
import com.ua.estore.cgsWeb.models.shipping.ShippingEstimate;
import com.ua.estore.cgsWeb.models.shop.Cart;
import com.ua.estore.cgsWeb.models.shop.Order;
import com.ua.estore.cgsWeb.models.shop.Product;
import com.ua.estore.cgsWeb.models.user.PaymentCard;
import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.models.vendor.Vendor;
import com.ua.estore.cgsWeb.services.mail.OrderConfirmationMailer;
import com.ua.estore.cgsWeb.services.shipping.ShippingEstimateService;
import com.ua.estore.cgsWeb.services.shipping.ShippingRateService;
import com.ua.estore.cgsWeb.services.shop.*;
import com.ua.estore.cgsWeb.services.vendor.VendorService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@AllArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final VendorService vendorService;
    private final ShippingRateService shippingRateService;
    private final ShippingEstimateService shippingEstimateService;
    private final OrderConfirmationMailer orderConfirmationMailer;


    /*****************************************************
     * View Checkout page
     ****************************************************/

    @GetMapping("/checkout")
    public String checkout(HttpSession session,
                           RedirectAttributes redirectAttributes,
                           Model model) {

        User user = (User) session.getAttribute("user");
        if(user == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Please login to checkout.");
            return "redirect:/login";
        }

        //Retrieve user cart to recalc total
        List<ProductDTO> cartItems = cartService.mapToProductDTOs(cartService.getOrCreateByUserId(user.getId()),
                productService, vendorService, categoryService);

        // Build a vendorId -> Vendor map for cart
        Map<String, Vendor> cartVendors = cartItems.stream()
                .map(ProductDTO::getVendorId)
                .distinct()
                .map(vendorService::getVendorById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Vendor::getId, v -> v, (a, b) -> a, LinkedHashMap::new));


        BigDecimal subtotal = cartItems.stream()
                .map(p -> {
                    BigDecimal unit = p.getSalePrice() != null && p.getSalePrice().signum() > 0
                            ? p.getSalePrice() : p.getPrice();
                    return unit.multiply(BigDecimal.valueOf(p.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        //Convert user addresses to json for address preview update .js
        String jsonAddresses = user.getAddresses().stream()
                .map(address -> String.format("{\"addressId\": \"%s\", \"street1\": \"%s\", " +
                                "\"street2\": \"%s\", \"city\": \"%s\", \"state\": \"%s\", \"zip\": \"%s\"}",
                        address.getAddressId(), address.getStreet1(),
                        address.getStreet2(), address.getCity(),
                        address.getState(), address.getZip()))
                .collect(Collectors.joining(", ", "[", "]"));


        //Site Model attributes
        model.addAttribute("jsonAddresses", jsonAddresses);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartVendors", cartVendors);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("taxRate", orderService.getTaxRate());

        return "shop/checkout";
    }

    /*****************************************************
     * Submit Order
     ****************************************************/

    @PostMapping("/checkout/submit")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkoutSubmit(
            @RequestParam("selectedAddress") String selectedAddress,
            @RequestParam(value = "deliveryInstructions", required = false) String deliveryInstructions,
            @RequestParam(value = "idempotencyKey", required = false) String clientIdempotencyKey,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return jsonError(HttpStatus.UNAUTHORIZED, "Please login again to complete checkout.");
        }

        // Re-load cart from DB so the form can't claim items the user no longer has.
        Cart cart = cartService.getOrCreateByUserId(user.getId());
        List<ProductDTO> cartItems = cartService.mapToProductDTOs(cart,
                productService, vendorService, categoryService);

        if (cartItems.isEmpty()) {
            return jsonError(HttpStatus.BAD_REQUEST, "Your cart is empty.");
        }

        // Resolve the selected address from the user's address book.
        Address shipTo = user.getAddresses().stream()
                .filter(a -> selectedAddress != null && selectedAddress.equals(a.getAddressId()))
                .findFirst()
                .orElse(null);
        if (shipTo == null) {
            return jsonError(HttpStatus.BAD_REQUEST, "Selected delivery address could not be found.");
        }

        // Server-authoritative shipping price – we do NOT trust whatever the form posted.
        // estimateForCart returns a single-element list in the Phase-1 single-warehouse model.
        List<ShippingEstimate> estimates = shippingEstimateService.estimateForCart(cart, shipTo);
        BigDecimal shippingCost = estimates.isEmpty() ? BigDecimal.ZERO : estimates.get(0).getCost();
        String rateCardId = shippingRateService.getActiveRateCard().getId();

        // Idempotency key: client-generated when present (modal flow), random fallback otherwise.
        UUID idempotencyKey = parseUuidOrRandom(clientIdempotencyKey);

        // Build the DTO and persist.
        OrderDTO orderTracker = OrderDTO.builder()
                .user(user)
                .idempotencyKey(idempotencyKey)
                .products(cartItems)
                .description(trimToMaxLen(deliveryInstructions, 500))
                .build();

        try {
            String orderId = orderService.savePendingOrder(orderTracker, shipTo, rateCardId, shippingCost);
            if (orderId == null) {
                return jsonError(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Unable to save your order. Please try again.");
            }

            cartService.clearCart(user.getId());
            Order saved = orderService.getOrderById(orderId);  // re-read so we have orderNumber

            if(saved != null) {
                orderConfirmationMailer.sendFor(saved);
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("orderId", orderId);
            body.put("orderNumber", saved != null ? saved.getOrderNumber() : null);
            body.put("redirect", "/checkout/confirmation/" + orderId);
            return ResponseEntity.ok(body);

        } catch (IllegalArgumentException ex) {
            log.warn("Checkout submit rejected: {}", ex.getMessage());
            return jsonError(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error during checkout submit for userId={}", user.getId(), ex);
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Something went wrong while placing your order. Please try again.");
        }
    }


    /*****************************************************
     * Order Confirmation page
     ****************************************************/

    @GetMapping("/checkout/confirmation/{orderId}")
    public String confirmation(@PathVariable("orderId") String orderId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Please login to view your order.");
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderService.getOrderForUser(orderId, user.getId());
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "We couldn't find that order. If you just placed it, please refresh.");
            return "redirect:/account?tab=orders";
        }

        Order order = orderOpt.get();

        // Rebuild a vendor map for the page (same shape as the checkout page expects, so the
        // confirmation template can iterate items grouped by vendor).
        Map<String, Vendor> orderVendors = order.getItems().stream()
                .map(Order.OrderItem::getVendorId)
                .distinct()
                .map(vendorService::getVendorById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Vendor::getId, v -> v, (a, b) -> a, LinkedHashMap::new));

        List<ProductDTO> cartItems = cartService.mapToProductDTOs(
                cartService.getOrCreateByUserId(user.getId()),
                productService, vendorService, categoryService);
        int cartCount = cartItems.stream()
                .mapToInt(ProductDTO::getQuantity)
                .sum();

        List<Product> recommendations = productService.recommendForOrder(order, 6);

        model.addAttribute("order", order);
        model.addAttribute("orderVendors", orderVendors);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("cancelWindowSeconds", OrderService.CANCEL_WINDOW.toSeconds());
        model.addAttribute("recommendations", recommendations);
        return "shop/order-confirm";
    }


    /***********************************************************
     * Cancel Order
     ***********************************************************/

    @PostMapping("/checkout/cancel/{orderId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable("orderId") String orderId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return jsonError(HttpStatus.UNAUTHORIZED,
                    "Please login to cancel an order.");
        }

        try {
            Order cancelled = orderService.cancelOrder(orderId, user.getId());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("cancelled", true);
            body.put("orderId", cancelled.getId());
            body.put("orderNumber", cancelled.getOrderNumber());
            return ResponseEntity.ok(body);

        } catch (IllegalArgumentException ex) {
            log.warn("Cancel rejected: orderId={} userId={} reason={}",
                    orderId, user.getId(), ex.getMessage());
            return jsonError(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error cancelling orderId={} for userId={}",
                    orderId, user.getId(), ex);
            return jsonError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Something went wrong cancelling your order. Please contact support.");
        }
    }


    /* =========================================================================
     * Internals
     * ======================================================================= */

    private static ResponseEntity<Map<String, Object>> jsonError(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }

    private static UUID parseUuidOrRandom(String raw) {
        if (raw == null || raw.isBlank()) return UUID.randomUUID();
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("Ignoring malformed client idempotencyKey='{}'; generating server-side.", raw);
            return UUID.randomUUID();
        }
    }

    private static String trimToMaxLen(String s, int maxLen) {
        if (s == null) return null;
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen);
    }
}
