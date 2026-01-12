package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.services.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final ProductService productService;

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        List<Product> cart = (List<Product>) session.getAttribute("cartItems");
        if (cart == null) {
            cart = new ArrayList<Product>();
        }

        System.out.println("Viewing Cart. Items count: " + cart.size());
        model.addAttribute("cartItems", cart);
        return "cart";
    }

    @GetMapping("/cart/add/{id}")
    public String addToCart(@PathVariable String id, HttpSession session) {
        // Retrieve cart from session, not model attribute
        List<Product> cart = (List<Product>) session.getAttribute("cartItems");

        if (cart == null) {
            cart = new ArrayList<>();
        }

        // Check for null instead of using ifPresent
        Product product = productService.getProductById(id);
        System.out.println("Adding product: " + (product != null ? product.getName() : "NULL"));
        if (product != null) {
            cart.add(product);
        }

        // Save updated cart back to session
        session.setAttribute("cartItems", cart);
        System.out.println("Cart Contents: ");
        for(Product p : cart) {
            System.out.printf("{ %s : %s : %s }\n", p.getName(), p.getId(), p.getPrice());
        }

        return "redirect:/shop";
    }
}
