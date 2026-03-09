package com.ua.estore.cgsWeb.controllers.shop;

import com.squareup.square.types.CreatePaymentResponse;
import com.squareup.square.types.Currency;
import com.ua.estore.cgsWeb.models.*;
import com.ua.estore.cgsWeb.models.dto.product.ProductDTO;
import com.ua.estore.cgsWeb.models.dto.shop.OrderDTO;
import com.ua.estore.cgsWeb.services.shipping.RoadieService;
import com.ua.estore.cgsWeb.services.shop.*;
import com.ua.estore.cgsWeb.services.vendor.VendorService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final SquareService squareService;
    private final RoadieService roadieService;


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

        //Validate Square profile created for user and Get Saved Payment Methods
        if(user.getSquareProfile() != null) {
            if (squareService.squareCustomerExists(user)) {
                List<PaymentCard> savedUserCards = squareService.getUserCards(user);
                model.addAttribute("savedUserCards", savedUserCards != null ? savedUserCards : new ArrayList<>());
                model.addAttribute("squareCustomerExists", true);
            } else {
                model.addAttribute("squareCustomerExists", false);
            }
        } else {
            model.addAttribute("squareCustomerExists", false);
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

        //Square Model attributes
        model.addAttribute("squareAppId", squareService.getApplicationId());
        model.addAttribute("squareLocationId", squareService.getLocationId());

        return "shop/checkout";
    }

    /*****************************************************
     * Checkout
     ****************************************************/

    @PostMapping("/checkout/submit")
    public String checkoutSubmit(@RequestParam("sourceId") String sourceId,
                                 @RequestParam("totalCents") long totalCents,
                                 @RequestParam(value = "tipCents", defaultValue = "0") long tipCents,
                                 @RequestParam("selectedAddress") String selectedAddress,
                                 RedirectAttributes redirectAttributes,
                                 HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unable to complete checkout. Please login again.");
            return "redirect:/login";
        }

        //ReRetrieve cart from DB just in case and calc total
        List<ProductDTO> cartItems = cartService.mapToProductDTOs(cartService.getOrCreateByUserId(user.getId()),
                productService, vendorService, categoryService);

        BigDecimal subtotal = cartItems.stream()
                .map(item -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        //Generate Idempotency Key for Order
        UUID idempotencyKey = UUID.randomUUID();

        //Create Pending Order and Save to DB
        OrderDTO orderTracker = OrderDTO.builder()
                .user(user)
                .idempotencyKey(idempotencyKey)
                .products(cartItems)
                .subtotalPrice(subtotal)
                .build();

        String orderId = orderService.savePendingOrder(orderTracker);

        //Update DTO with returned ID escape if no id/unsuccessful
        if (orderId != null) {
            orderTracker.setOrderId(orderId);
        } else {
            redirectAttributes.addFlashAttribute("error", "Unable to complete checkout. Please try again later.");
            return "redirect:/checkout";
        }

        //Call Roadie Create Shipment // Need finalized shipping estimates for square payment
        Map<String, Object> shipments = roadieService.createShipment(orderTracker, selectedAddress);

        if (!shipments.containsKey("error")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> shipmentList = (List<Map<String, Object>>) shipments.get("shipments");

            List<OrderDTO.Shipment> roadieShipments = new ArrayList<>();
            if (shipmentList != null) {
                for (Map<String, Object> entry : shipmentList) {
                    OrderDTO.Shipment shipment = new OrderDTO.Shipment();
                    shipment.setOrderId(String.valueOf(entry.get("orderId")));
                    shipment.setTrackingNumber(String.valueOf(entry.get("tracking")));
                    roadieShipments.add(shipment);
                }
            }

            orderTracker.setRoadieShipments(roadieShipments);
            orderService.saveShipmentCreatedOrder(orderTracker);

        } else {
            log.error("Error creating Roadie shipment: " + shipments.get("error"));
            redirectAttributes.addFlashAttribute("error", "Unable to complete checkout. Error creating Roadie shipment.");
            return "redirect:/checkout";
        }


        //Call Square Create Payment
        CreatePaymentResponse payResponse = squareService.createPayment(sourceId, idempotencyKey.toString(),
                subtotal.multiply(BigDecimal.valueOf(100)).longValue(), tipCents, Currency.USD);



        return "redirect:/checkout";
    }
}
