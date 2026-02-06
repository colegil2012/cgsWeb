package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.dto.ProductDTO;
import com.ua.estore.cgsWeb.services.CartService;
import com.ua.estore.cgsWeb.services.CategoryService;
import com.ua.estore.cgsWeb.services.ProductService;
import com.ua.estore.cgsWeb.services.VendorService;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@AllArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final VendorService vendorService;

    @GetMapping("/checkout")
    public String checkout(HttpSession session,
                           @RequestParam(name = "selectedAddress") String selectedAddress) {

        User user = (User) session.getAttribute("user");
        if(user == null) {
            session.setAttribute("error", "Please login to checkout.");
            return "redirect:/login";
        }

        List<ProductDTO> cartItems = cartService.mapToProductDTOs(
                cartService.getOrCreateByUserId(user.getId()),
                productService,
                vendorService,
                categoryService);



        return "shop/checkout";
    }
}
