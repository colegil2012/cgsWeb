package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.services.ProductService;
import com.ua.estore.cgsWeb.util.searchUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ShopController {

    private final ProductService productService;

    @GetMapping("/shop")
    public String shop(HttpSession session, Model model) {
        clearFilterCache(session);
        List<Product> products = productService.getAllProducts();
        List<String> categories = searchUtil.getCategories(products);

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);

        return "shop";
    }

    @GetMapping("/shop/filter")
    public String filterProducts(@RequestParam(required = false) String category,
                                 @RequestParam(required = false) String search,
                                 @RequestParam(name = "lowStock", defaultValue = "false") boolean lowStock,
                                 HttpSession session,
                                 Model model) {

        session.setAttribute("lastCategory", category);
        session.setAttribute("lastSearch", search);
        session.setAttribute("lastLowStock", lowStock);

        return executeFiltering(category, search, lowStock, model);
    }

    /*
       Helper Methods
     */

    private String executeFiltering(String category, String search, boolean lowStock, Model model) {
        List<Product> allProducts = productService.getAllProducts();
        List<String> categories = searchUtil.getCategories(allProducts);
        List<Product> filteredProducts = productService.getProductsByFilter(category, search, lowStock);

        model.addAttribute("products", filteredProducts);
        model.addAttribute("categories", categories);
        model.addAttribute("lastCategory", category);
        model.addAttribute("lastSearch", search);
        model.addAttribute("lastLowStock", lowStock);
        return "shop";
    }

    public void clearFilterCache(HttpSession session) {
        session.removeAttribute("lastCategory");
        session.removeAttribute("lastSearch");
        session.removeAttribute("lastLowStock");
        session.removeAttribute("currentCategory");
        session.removeAttribute("currentSearch");
        session.removeAttribute("currentLowStock");
    }
}
