package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.dto.ProductDTO;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("username")
    public String addUserToModel( @SessionAttribute(name = "username", required = false)
                                      String username) {
        return username;
    }

    @ModelAttribute("role")
    public String addRoleToModel( @SessionAttribute(name = "role", required = false)
                                      String role) {
        return role;
    }

    @ModelAttribute("cartItems")
    public List<ProductDTO> addCartToModel(@SessionAttribute(name = "cartItems", required = false)
                                         List<ProductDTO> cart) {
        return cart != null ? cart : new ArrayList<>();
    }


}
