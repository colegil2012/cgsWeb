
package com.ua.estore.cgsWeb.controllers.main;

import com.ua.estore.cgsWeb.models.Cart;
import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.services.shop.CartService;
import com.ua.estore.cgsWeb.services.user.GuestIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final CartService cartService;
    private final GuestIdentityService guestIdentityService;

    @Value("${app.images.base-url:}")
    private String imagesBaseUrl;

    @ModelAttribute("user")
    public User addUserToModel(@SessionAttribute(name = "user", required = false) User user) {
        return user;
    }

    @ModelAttribute("userCart")
    public Cart addCartToModel(
            @SessionAttribute(name = "user", required = false) User user,
            @SessionAttribute(name = "userCart", required = false) Cart userCart,
            HttpSession session,
            HttpServletRequest request) {

        if (user != null && user.getId() != null) {
            if (userCart == null) {
                Cart fresh = cartService.getOrCreateByUserId(user.getId());
                session.setAttribute("userCart", fresh);
                return fresh;
            }
            return userCart;
        }

        // Guest: use the guest id cookie if one exists, but don't mint one until they add something.
        String guestId = guestIdentityService.readGuestId(request);
        if (guestId == null) {
            return new Cart(); // empty, transient
        }
        Cart guestCart = cartService.getOrCreateByGuestId(guestId);
        session.setAttribute("userCart", guestCart);
        return guestCart;
    }

    @ModelAttribute("cartCount")
    public int addCartCountToModel(@ModelAttribute("userCart") Cart cart) {
        return cart == null ? 0 : cart.totalQuantity();
    }

    @ModelAttribute
    public void addGlobals(Model model) {
        model.addAttribute("imagesBaseUrl", imagesBaseUrl);
    }
}