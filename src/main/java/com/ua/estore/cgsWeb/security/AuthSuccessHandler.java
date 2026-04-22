package com.ua.estore.cgsWeb.security;

import com.ua.estore.cgsWeb.models.Cart;
import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.repositories.UserRepository;
import com.ua.estore.cgsWeb.services.shop.CartService;
import com.ua.estore.cgsWeb.services.user.GuestIdentityService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * After Spring Security authenticates a user, we:
 *   1. Load the full User from Mongo and place it in session under "user"
 *      so existing controllers keep working unchanged.
 *   2. Merge any guest cart (tracked by cookie) into the user's cart.
 *   3. Populate "userCart" and "cartCount" in session.
 *   4. Redirect to the saved request (if any) or "/".
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final CartService cartService;
    private final GuestIdentityService guestIdentityService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws ServletException, IOException {

        if (authentication.getPrincipal() instanceof CustomUserDetails principal) {
            userRepository.findById(principal.getUserId()).ifPresent(fresh -> {
                // Strip password before storing in session
                fresh.setPassword(null);
                bootstrapSession(request, fresh);
            });
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private void bootstrapSession(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        session.setAttribute("user", user);

        // Merge guest cart if present, otherwise just load user's cart
        String guestId = guestIdentityService.readGuestId(request);
        Cart cart = (guestId != null)
                ? cartService.mergeGuestCartIntoUser(guestId, user.getId())
                : cartService.getOrCreateByUserId(user.getId());

        session.setAttribute("userCart", cart);
        session.setAttribute("cartCount", cart.totalQuantity());

        if (guestId != null) {
            // Tell the browser to drop the guest cookie — done in a filter
            // that runs after this handler (see AuthSuccessHandler usage in SecurityConfig).
            request.setAttribute("cgs.clearGuestCookie", Boolean.TRUE);
        }

        log.info("User {} authenticated. Cart has {} items.",
                user.getUsername(), cart.totalQuantity());
    }
}