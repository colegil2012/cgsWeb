package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.Cart;
import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.services.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final CartService cartService;

    @Value("${app.images.base-url:}")
    private String imagesBaseUrl;

    public GlobalModelAdvice(CartService cartService) {
        this.cartService = cartService;
    }

    @ModelAttribute("user")
    public User addUserToModel( @SessionAttribute(name = "user", required = false)
                                  User user) {
        return user;
    }

    @ModelAttribute("userCart")
    public Cart addCartToModel(
            @SessionAttribute(name = "user", required = false) User user,
            @SessionAttribute(name = "userCart", required = false) Cart userCart,
            HttpSession session) {

        if (user == null || user.getId() == null) {
            return userCart != null ? userCart : new Cart();
        }

        // If session cart is missing, load from DB and store it for future requests
        if (userCart == null) {
            Cart fresh = cartService.getOrCreateByUserId(user.getId());
            session.setAttribute("userCart", fresh);
            return fresh;
        }

        return userCart;
    }

    @ModelAttribute("cartCount")
    public int addCartCountToModel(
            @SessionAttribute(name = "user", required = false) User user,
            @SessionAttribute(name = "userCart", required = false) Cart cart,
            HttpSession session) {

        if (user == null || user.getId() == null) {
            return 0;
        }

        if (cart == null || cart.getUserId() == null || !user.getId().equals(cart.getUserId())) {
            Cart fresh = cartService.getOrCreateByUserId(user.getId());
            session.setAttribute("userCart", fresh);
            return fresh.totalQuantity();
        }

        return cart.totalQuantity();
    }

    @ModelAttribute
    public void addGlobals(Model model) {

        model.addAttribute("imagesBaseUrl", imagesBaseUrl);
    }

}
