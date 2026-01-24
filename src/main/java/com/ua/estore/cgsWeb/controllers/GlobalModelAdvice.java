package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.dto.ProductDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalModelAdvice {

    @Value("${app.images.base-url:}")
    private String imagesBaseUrl;

    @ModelAttribute("user")
    public User addUserToModel( @SessionAttribute(name = "user", required = false)
                                  User user) {
        return user;
    }

    @ModelAttribute("cartItems")
    public List<ProductDTO> addCartToModel(@SessionAttribute(name = "cartItems", required = false)
                                         List<ProductDTO> cart) {
        return cart != null ? cart : new ArrayList<>();
    }

    @ModelAttribute
    public void addGlobals(Model model) {
        model.addAttribute("imagesBaseUrl", imagesBaseUrl);
    }

}
