package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.CartItem;
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
import java.util.Optional;

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
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cartItems");

        if (cart == null) {
            cart = new ArrayList<>();
        }

        // Find if product already exists in cart
        Optional<CartItem> existingItem = cart.stream()
                .filter(item -> item.getProduct().getId().equals(id))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().increaseQuantity();
        } else {
            Product product = productService.getProductById(id);
            if (product != null) {
                cart.add(new CartItem(product, 1));
            }
        }

        session.setAttribute("cartItems", cart);

        return "redirect:/shop";
    }
}
