package com.ua.estore.cgsWeb.controllers.shipping;

import com.ua.estore.cgsWeb.models.Address;
import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.Vendor;
import com.ua.estore.cgsWeb.models.dto.product.ProductDTO;
import com.ua.estore.cgsWeb.models.dto.roadie.*;
import com.ua.estore.cgsWeb.models.dto.roadie.Package;
import com.ua.estore.cgsWeb.models.dto.shop.OrderDTO;
import com.ua.estore.cgsWeb.models.wrappers.PackageWrapper;
import com.ua.estore.cgsWeb.services.shop.CartService;
import com.ua.estore.cgsWeb.services.shop.CategoryService;
import com.ua.estore.cgsWeb.services.shop.ProductService;
import com.ua.estore.cgsWeb.services.vendor.VendorService;
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
     * Called from cart.js
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
        List<ProductDTO> cartItems = cartService.mapToProductDTOs(cartService.getOrCreateByUserId(user.getId()),
                productService, vendorService, categoryService);

        Map<String, Object> estimates = roadieService.getShippingEstimates(user, cartItems, shippingAddressId);

        if(estimates.containsKey("error")) {
            return ResponseEntity.badRequest().body(estimates);
        }

        return ResponseEntity.ok(estimates);
    }
}
