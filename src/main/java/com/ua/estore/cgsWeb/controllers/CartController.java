package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.Vendor;
import com.ua.estore.cgsWeb.models.dto.ProductDTO;
import com.ua.estore.cgsWeb.models.dto.RoadieEstimateRequest;
import com.ua.estore.cgsWeb.models.wrappers.PackageWrapper;
import com.ua.estore.cgsWeb.services.CategoryService;
import com.ua.estore.cgsWeb.services.ProductService;
import com.ua.estore.cgsWeb.services.shipping.RoadieService;
import com.ua.estore.cgsWeb.services.VendorService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CartController {

    private final ProductService productService;
    private final VendorService vendorService;
    private final CategoryService categoryService;
    private final RoadieService roadieService;

    /**********************************************************************************
     * Controller methods for handling cart-related operations
     *********************************************************************************/

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        List<ProductDTO> cart = (List<ProductDTO>) session.getAttribute("cartItems");
        User user = (User) session.getAttribute("user");;

        if (cart == null) {
            cart = new ArrayList<>();
        }

        for (ProductDTO item : cart) {
            totalPrice = totalPrice.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // --- Roadie Multi-Vendor Shipping Logic using PackageWrapper ---
        List<Map<String, Object>> shippingEstimates = new ArrayList<>();
        BigDecimal totalShipping = BigDecimal.ZERO;

        if (!cart.isEmpty()) {
            // 1. Group items into PackageWrappers by Vendor
            Map<String, PackageWrapper> vendorPackages = new HashMap<>();

            for (ProductDTO item : cart) {
                Product p = productService.getProductById(item.getId());
                if (p != null) {
                    vendorPackages.computeIfAbsent(p.getVendorId(),
                                    id -> new PackageWrapper(id, item.getVendorName()))
                            .addItem(item, p);
                }
            }

            // 2. Iterate through packages and get estimates
            for (PackageWrapper pkg : vendorPackages.values()) {
                Vendor vendor = vendorService.getVendorById(pkg.getVendorId()).orElse(null);

                if (vendor != null) {
                    var pickupAddr = vendor.getAddresses().stream()
                            .filter(Vendor.Address::isDefault).findFirst()
                            .orElse(vendor.getAddresses().isEmpty() ? null : vendor.getAddresses().get(0));

                    var deliveryAddr = user != null ? user.getAddresses().stream()
                            .filter(User.Address::isDefault).findFirst()
                            .orElse(user.getAddresses().isEmpty() ? null : user.getAddresses().get(0)) : null;


                    if (pickupAddr != null && deliveryAddr != null) {
                        int maxProductReadyTime = pkg.getItems().stream()
                                .map(item -> productService.getProductById(item.getId()))
                                .filter(Objects::nonNull)
                                .mapToInt(Product::getReadyTime)
                                .max()
                                .orElse(0);

                        // Total lead time = Vendor overhead + longest product prep time
                        int totalLeadTimeDays = vendor.getLead_time() + maxProductReadyTime;

                        log.info("Lead time for " + pkg.getVendorName() + ": " + totalLeadTimeDays + " days");

                        OffsetDateTime pickupTime = OffsetDateTime.now().plusDays(totalLeadTimeDays);
                        OffsetDateTime deliveryDeadline = pickupTime.plusDays(1);

                        String pickupAfterIso = pickupTime.toString();
                        String deliverBetweenStartIso = deliveryDeadline.toString();
                        String deliverBetweenEndIso = deliveryDeadline.plusHours(2).toString();



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
                                                .street1(deliveryAddr.getStreet())
                                                .city(deliveryAddr.getCity())
                                                .state(deliveryAddr.getState())
                                                .zip(deliveryAddr.getZip()).build())
                                        .build())
                                .pickupAfter(pickupAfterIso)
                                .deliverBetween(RoadieEstimateRequest.DeliveryWindow.builder()
                                        .start(deliverBetweenStartIso)
                                        .end(deliverBetweenEndIso)
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
            }
        }

        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("cartItems", cart);
        model.addAttribute("shippingEstimates", shippingEstimates);
        model.addAttribute("totalShipping", totalShipping);

        return "shop/cart";
    }

    /******* Add to cart ********************/

    @GetMapping("/cart/add/{id}")
    @ResponseBody
    public Object addToCart(@PathVariable String id, HttpSession session) {
        List<ProductDTO> cart = (List<ProductDTO>) session.getAttribute("cartItems");
        if (cart == null) {
            cart = new ArrayList<>();
        }

        Optional<ProductDTO> existingItem = cart.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().increaseQuantity();
        } else {
            Product product = productService.getProductById(id);
            if (product != null) {
                String vendorName = vendorService.getVendorNameMap()
                        .getOrDefault(product.getVendorId(), "Unknown Vendor");

                String categoryName = "Uncategorized";
                if (product.getCategoryId() != null) {
                    var cat = categoryService.getCategoryById(product.getCategoryId());
                    if (cat != null) categoryName = cat.getName();
                }

                ProductDTO dto = new ProductDTO(
                        product.getId(), product.getName(), product.getSlug(),
                        product.getDescription(), product.getPrice(), product.getSalePrice(),
                        categoryName, product.getImageUrl(), product.getVendorId(), vendorName,
                        product.getStock(), product.getLowStockThreshold(), 1
                );
                cart.add(dto);
            }
        }

        session.setAttribute("cartItems", cart);
        int totalCount = cart.stream().mapToInt(ProductDTO::getQuantity).sum();
        return Map.of("success", true, "cartCount", totalCount);
    }

    /******* Remove from cart ********************/

    @GetMapping("/cart/remove/{id}")
    @ResponseBody
    public Object removeFromCart(@PathVariable String id, HttpSession session) {
        List<ProductDTO> cart = (List<ProductDTO>) session.getAttribute("cartItems");

        if (cart != null) {
            Optional<ProductDTO> existingItem = cart.stream()
                    .filter(item -> item.getId().equals(id))
                    .findFirst();

            if (existingItem.isPresent()) {
                ProductDTO item = existingItem.get();
                if (item.getQuantity() > 1) {
                    item.decreaseQuantity();
                } else {
                    cart.remove(item);
                }
            }
            session.setAttribute("cartItems", cart);
        }

        int totalCount = (cart == null) ? 0 : cart.stream().mapToInt(ProductDTO::getQuantity).sum();
        return Map.of("success", true, "cartCount", totalCount);
    }
}