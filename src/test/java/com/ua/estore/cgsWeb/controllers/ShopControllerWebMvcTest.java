package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.controllers.shop.ShopController;
import com.ua.estore.cgsWeb.models.Cart;
import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.services.shop.CartService;
import com.ua.estore.cgsWeb.services.shop.CategoryService;
import com.ua.estore.cgsWeb.services.shop.ProductService;
import com.ua.estore.cgsWeb.services.vendor.VendorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static com.ua.estore.cgsWeb.support.TestUsers.sessionCartFor;
import static com.ua.estore.cgsWeb.support.TestUsers.sessionUser;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShopController.class)
public class ShopControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private VendorService vendorService;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private CartService cartservice;


    @Test
    @WithMockUser
    public void shopPageRendersTest() throws Exception {
        User user = sessionUser();
        Cart cart = sessionCartFor(user);

        when(cartservice.getOrCreateByUserId(user.getId())).thenReturn(cart);
        when(productService.getAllProducts()).thenReturn(List.of());
        when(vendorService.getAllVendors()).thenReturn(List.of());
        when(categoryService.getCategoryNameMap()).thenReturn(Map.of());

        // /shop also calls executeFiltering() -> productService.getProductsByFilter(...)
        Page<Product> emptyPage = new PageImpl<>(List.of());
        when(productService.getProductsByFilter(anyString(), anyString(), anyString(), anyBoolean(), anyInt()))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/shop")
                        .sessionAttr("user", user)
                        .sessionAttr("userCart", cart))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/shop"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("vendors"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    @WithMockUser
    public void shopPageExecuteFilteringTest() throws Exception {
        User user = sessionUser();
        Cart cart = sessionCartFor(user);

        when(cartservice.getOrCreateByUserId(user.getId())).thenReturn(cart);
        when(productService.getAllProducts()).thenReturn(List.of());
        when(vendorService.getAllVendors()).thenReturn(List.of());
        when(categoryService.getCategoryNameMap()).thenReturn(Map.of());

        // /shop also calls executeFiltering() -> productService.getProductsByFilter(...)
        Page<Product> emptyPage = new PageImpl<>(List.of());
        when(productService.getProductsByFilter(eq("grapes"), eq("produce"), eq("vendor123"), eq(true), eq(0)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/shop/filter")
                        .sessionAttr("user", user)
                        .sessionAttr("userCart", cart)
                        .param("search", "grapes")
                        .param("category", "produce")
                        .param("vendor", "vendor123")
                        .param("lowStock", "true")
                        .param("page", "0")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("shop/shop"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("vendors"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attribute("search", "grapes"))
                .andExpect(model().attribute("category", "produce"))
                .andExpect(model().attribute("vendor", "vendor123"))
                .andExpect(model().attribute("lowStock", true))
                .andExpect(model().attribute("currentPage", 0));;
    }
}
