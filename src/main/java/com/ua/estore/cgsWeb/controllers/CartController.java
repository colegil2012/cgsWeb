package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.dto.ProductDTO;
import com.ua.estore.cgsWeb.services.ProductService;
import com.ua.estore.cgsWeb.services.VendorService;
import com.ua.estore.cgsWeb.util.dataUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final ProductService productService;
    private final VendorService vendorService;


    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        BigDecimal totalPrice = new BigDecimal(0);

        List<ProductDTO> cart = (List<ProductDTO>) session.getAttribute("cartItems");
        if (cart == null) { cart = new ArrayList<>(); }

        for (ProductDTO item : cart) {
            totalPrice = totalPrice.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("cartItems", cart);
        return "cart";
    }


    @GetMapping("/cart/add/{id}")
    @ResponseBody
    public Object addToCart(@PathVariable String id, HttpSession session) {
        List<ProductDTO> cart = (List<ProductDTO>) session.getAttribute("cartItems");

        if (cart == null) {
            cart = new ArrayList<>();
        }

        Optional<ProductDTO> existingItem = cart.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(existingItem.get().getQuantity() + 1);
        } else {
            Product product = productService.getProductById(id);
            if (product != null) {
                // Map Product to ProductDTO for storage
                String vendorName = vendorService.getVendorNameMap()
                        .getOrDefault(dataUtil.parseToObjectId(product.getVendorId()).toHexString(), "Unknown Vendor");

                ProductDTO dto = new ProductDTO(
                        product.getId(), product.getName(), product.getDescription(),
                        product.getPrice(), product.getCategory(), product.getImageUrl(),
                        vendorName, 1
                );
                cart.add(dto);
            }
        }

        session.setAttribute("cartItems", cart);
        int totalCount = cart.stream().mapToInt(ProductDTO::getQuantity).sum();

        return Map.of("success", true, "cartCount", totalCount);
    }


}
