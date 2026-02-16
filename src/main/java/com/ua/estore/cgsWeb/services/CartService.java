package com.ua.estore.cgsWeb.services;

import com.ua.estore.cgsWeb.models.Cart;
import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.dto.product.ProductDTO;
import com.ua.estore.cgsWeb.repositories.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

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

    public int getCartCount(String userId) {
        return getOrCreateByUserId(userId).totalQuantity();
    }

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
