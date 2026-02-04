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
import com.ua.estore.cgsWeb.services.shipping.RoadieService;
import com.ua.estore.cgsWeb.services.VendorService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final CartService cartService;

    /**********************************************************************************
     * Controller methods for handling cart-related operations
     *********************************************************************************/

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        Cart cart = cartService.getOrCreateByUserId(user.getId());

        List<ProductDTO> cartItems = cartService.mapToProductDTOs(
                cart,
                productService,
                vendorService,
                categoryService
        );

        model.addAttribute("cartItems", cartItems);
        return "shop/cart";
    }

    /******* Add to cart ********************/

    @GetMapping("/cart/add/{id}")
    @ResponseBody
    public Object addToCart(@PathVariable String id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        Cart updated = cartService.addOne(user.getId(), id);
        session.setAttribute("userCart", updated);
        session.setAttribute("cartCount", updated.totalQuantity());

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("cartCount", updated.totalQuantity());
        return ResponseEntity.ok(body);
    }

    /******* Remove from cart ********************/

    @GetMapping("/cart/remove/{id}")
    @ResponseBody
    public Object removeFromCart(@PathVariable String id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        Cart updated = cartService.removeOne(user.getId(), id);
        session.setAttribute("userCart", updated);
        session.setAttribute("cartCount", updated.totalQuantity());

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("cartCount", updated.totalQuantity());
        return ResponseEntity.ok(body);
    }
}