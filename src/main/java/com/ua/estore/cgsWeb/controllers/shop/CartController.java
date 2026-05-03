package com.ua.estore.cgsWeb.controllers.shop;

import com.ua.estore.cgsWeb.models.shop.Cart;
import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.models.dto.product.ProductDTO;
import com.ua.estore.cgsWeb.services.shop.CartService;
import com.ua.estore.cgsWeb.services.shop.CategoryService;
import com.ua.estore.cgsWeb.services.shop.ProductService;
import com.ua.estore.cgsWeb.services.user.GuestIdentityService;
import com.ua.estore.cgsWeb.services.vendor.VendorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CartController {

    private final ProductService productService;
    private final VendorService vendorService;
    private final CategoryService categoryService;
    private final CartService cartService;
    private final GuestIdentityService guestIdentityService;

    /**********************************************************************************
     * View cart — works for both authenticated users and guests
     *********************************************************************************/

    @GetMapping("/cart")
    public String viewCart(HttpSession session,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           Model model) {
        Cart cart = resolveCurrentCart(session, request, response, /*mintIfMissing*/ false);

        List<ProductDTO> cartItems = cartService.mapToProductDTOs(
                cart, productService, vendorService, categoryService
        );

        model.addAttribute("cartItems", cartItems);
        return "shop/cart";
    }

    /******* Add to cart (POST) ********************/

    @PostMapping("/cart/add/{id}")
    @ResponseBody
    public ResponseEntity<?> addToCart(@PathVariable String id,
                                       HttpSession session,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        User user = (User) session.getAttribute("user");
        Cart updated;

        if (user != null && user.getId() != null) {
            updated = cartService.addOne(user.getId(), id);
        } else {
            String guestId = guestIdentityService.ensureGuestId(request, response);
            updated = cartService.addOneGuest(guestId, id);
        }

        session.setAttribute("userCart", updated);
        session.setAttribute("cartCount", updated.totalQuantity());

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("cartCount", updated.totalQuantity());
        return ResponseEntity.ok(body);
    }

    /******* Remove from cart (POST) ********************/

    @PostMapping("/cart/remove/{id}")
    @ResponseBody
    public ResponseEntity<?> removeFromCart(@PathVariable String id,
                                            HttpSession session,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        User user = (User) session.getAttribute("user");
        Cart updated;

        if (user != null && user.getId() != null) {
            updated = cartService.removeOne(user.getId(), id);
        } else {
            String guestId = guestIdentityService.ensureGuestId(request, response);
            updated = cartService.removeOneGuest(guestId, id);
        }

        session.setAttribute("userCart", updated);
        session.setAttribute("cartCount", updated.totalQuantity());

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("cartCount", updated.totalQuantity());
        return ResponseEntity.ok(body);
    }

    /* ---------- helper ---------- */

    private Cart resolveCurrentCart(HttpSession session,
                                    HttpServletRequest request,
                                    HttpServletResponse response,
                                    boolean mintIfMissing) {
        User user = (User) session.getAttribute("user");
        if (user != null && user.getId() != null) {
            return cartService.getOrCreateByUserId(user.getId());
        }
        String guestId = mintIfMissing
                ? guestIdentityService.ensureGuestId(request, response)
                : guestIdentityService.readGuestId(request);
        return guestId == null ? new Cart() : cartService.getOrCreateByGuestId(guestId);
    }
}