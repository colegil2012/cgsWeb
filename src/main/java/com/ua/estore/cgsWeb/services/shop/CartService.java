package com.ua.estore.cgsWeb.services.shop;

import com.ua.estore.cgsWeb.config.props.SecurityProperties;
import com.ua.estore.cgsWeb.models.shop.Cart;
import com.ua.estore.cgsWeb.models.shop.Product;
import com.ua.estore.cgsWeb.models.dto.product.ProductDTO;
import com.ua.estore.cgsWeb.repositories.shop.CartRepository;
import com.ua.estore.cgsWeb.services.vendor.VendorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final SecurityProperties securityProperties;

    /* ---------- User carts ---------- */

    public Cart getOrCreateByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id is required.");
        }
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(userId)));
    }

    public Cart addOne(String userId, String productId) {
        Cart cart = getOrCreateByUserId(userId);
        cart.addProduct(productId);
        return cartRepository.save(cart);
    }

    public Cart removeOne(String userId, String productId) {
        Cart cart = getOrCreateByUserId(userId);
        cart.removeOne(productId);
        return cartRepository.save(cart);
    }

    public int getCartCount(String userId)
    {
        return getOrCreateByUserId(userId).totalQuantity();
    }

    public void clearCart(String userId) {
        if (userId == null || userId.isBlank()) return;
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cartRepository.delete(cart);
            log.info("Cleared cart for user {} (was {} items)", userId, cart.totalQuantity());
        });
    }

    /* ---------- Guest carts ---------- */

    public Cart getOrCreateByGuestId(String guestId) {
        if (guestId == null || guestId.isBlank()) {
            throw new IllegalArgumentException("Guest id is required.");
        }
        return cartRepository.findByGuestId(guestId)
                .map(this::refreshGuestExpiry)
                .orElseGet(() -> cartRepository.save(Cart.forGuest(guestId, newGuestExpiry())));
    }

    public Cart addOneGuest(String guestId, String productId) {
        Cart cart = getOrCreateByGuestId(guestId);
        cart.addProduct(productId);
        cart.setExpiresAt(newGuestExpiry());
        return cartRepository.save(cart);
    }

    public Cart removeOneGuest(String guestId, String productId) {
        Cart cart = getOrCreateByGuestId(guestId);
        cart.removeOne(productId);
        cart.setExpiresAt(newGuestExpiry());
        return cartRepository.save(cart);
    }

    /**
     * Merge the guest cart into the user's cart. The guest cart is deleted after merging.
     * If the user has no cart yet, it's created. Safe to call when no guest cart exists.
     */
    public Cart mergeGuestCartIntoUser(String guestId, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id is required.");
        }
        Cart userCart = getOrCreateByUserId(userId);

        if (guestId == null || guestId.isBlank()) return userCart;

        var guestCartOpt = cartRepository.findByGuestId(guestId);
        if (guestCartOpt.isEmpty()) return userCart;

        Cart guestCart = guestCartOpt.get();
        if (guestCart.getItems() != null) {
            for (Cart.Item item : guestCart.getItems()) {
                if (item == null || item.getProductId() == null || item.getQuantity() == null) continue;
                userCart.addProduct(item.getProductId(), item.getQuantity());
            }
        }

        Cart saved = cartRepository.save(userCart);
        cartRepository.delete(guestCart);
        log.info("Merged guest cart {} into user {}'s cart ({} items).",
                guestId, userId, saved.totalQuantity());
        return saved;
    }

    private Instant newGuestExpiry() {
        int days = securityProperties.guestCart().maxAgeDays();
        return Instant.now().plus(days, ChronoUnit.DAYS);
    }

    private Cart refreshGuestExpiry(Cart cart) {
        cart.setExpiresAt(newGuestExpiry());
        return cartRepository.save(cart);
    }

    /* ---------- Mapping helpers (unchanged) ---------- */

    public List<ProductDTO> mapToProductDTOs(Cart cart,
                                             ProductService productService,
                                             VendorService vendorService,
                                             CategoryService categoryService) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> vendorMap = vendorService.getVendorNameMap();
        Map<String, String> categoryMap = categoryService.getCategoryNameMap();

        return cart.getItems().stream()
                .filter(i -> i != null && i.getProductId() != null && !i.getProductId().isBlank())
                .map(item -> {
                    Product p = productService.getProductById(item.getProductId());
                    if (p == null) return null;

                    String vName = vendorMap.getOrDefault(p.getVendorId(), "Unknown Vendor");
                    String cName = categoryMap.getOrDefault(p.getCategoryId(), "Uncategorized");

                    int qty = (item.getQuantity() == null) ? 0 : item.getQuantity();

                    return new ProductDTO(
                            p.getId(),
                            p.getName(),
                            p.getSlug(),
                            p.getDescription(),
                            p.getPrice(),
                            p.getSalePrice(),
                            cName,
                            p.getImageUrl(),
                            p.getVendorId(),
                            vName,
                            p.getStock() == null ? 0 : p.getStock(),
                            p.getLowStockThreshold() == null ? 0 : p.getLowStockThreshold(),
                            qty
                    );
                })
                .filter(dto -> dto != null && dto.getQuantity() > 0)
                .toList();
    }
}