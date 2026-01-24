package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.Vendor;
import com.ua.estore.cgsWeb.models.dto.ProductDTO;
import com.ua.estore.cgsWeb.services.CategoryService;
import com.ua.estore.cgsWeb.services.ProductService;
import com.ua.estore.cgsWeb.services.VendorService;
import com.ua.estore.cgsWeb.util.dataUtil;
import com.ua.estore.cgsWeb.util.requestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ShopController {

    private final ProductService productService;
    private final VendorService vendorService;
    private final CategoryService categoryService;

    @Value("${app.images.base-url:}")
    private String imagesBaseUrl;

    /**********************************************************************************
     * Controller methods for handling shop-related operations
     *********************************************************************************/

    @GetMapping("/shop")
    public String shop(@RequestParam(defaultValue = "0") int page,
                       Model model, HttpSession session, HttpServletRequest request) {

        session.setAttribute("backLinkText", "← Back to Shop");
        session.setAttribute("backLinkUrl", requestUtil.buildFullUrl(request));

        List<Product> products = productService.getAllProducts();
        List<Vendor> vendors = vendorService.getAllVendors();
        Map<String, String> categories = categoryService.getCategoryNameMap();

        model.addAttribute("vendors", vendors);
        model.addAttribute("products", dataUtil.convertToProductDto(products, vendorService, categories));
        model.addAttribute("categories", categories);

        return executeFiltering("", "", "", false, page, categories, model);
    }

    @GetMapping("/shop/filter")
    public String filterProducts(@RequestParam(required = false) String search,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(name = "vendor", required = false) String vendor,
                                 @RequestParam(name = "lowStock", defaultValue = "false") boolean lowStock,
                                 @RequestParam(defaultValue = "0") int page,
                                 Model model, HttpSession session, HttpServletRequest request) {

        session.setAttribute("backLinkText", "← Back to Shop");
        session.setAttribute("backLinkUrl", requestUtil.buildFullUrl(request));

        return executeFiltering(search, category, vendor, lowStock, page, categoryService.getCategoryNameMap(), model);
    }

    @GetMapping("/shop/view/{id}")
    public String viewProduct(@PathVariable String id, Model model, HttpSession session) {
        Product product = productService.getProductById(id);
        Map<String, String> categories = categoryService.getCategoryNameMap();
        if( product == null ) return "redirect:/shop";

        List<ProductDTO> dtos = dataUtil.convertToProductDto(List.of(product), vendorService, categories);
        if (dtos.isEmpty()) return "redirect:/shop";

        ProductDTO productDto = dtos.getFirst();

        String backHref = (String) session.getAttribute("backLinkHref");
        String backText = (String) session.getAttribute("backLinkText");

        model.addAttribute("imagesBaseUrl", imagesBaseUrl);
        model.addAttribute("selected_product", productDto);
        model.addAttribute("backLinkHref", backHref != null ? backHref : "/shop");
        model.addAttribute("backLinkText", backText != null ? backText : "← Back to Shop");

        return "shop/product";
    }



    /********************************************************************************
       Helper Methods
     *******************************************************************************/


    private String executeFiltering(String search, String category, String vendor, boolean lowStock, int page, Map<String, String> catMap, Model model) {
        Page<Product> productPage = productService.getProductsByFilter(search, category, vendor, lowStock, page);

        model.addAttribute("imagesBaseUrl", imagesBaseUrl);
        model.addAttribute("products", dataUtil.convertToProductDto(productPage.getContent(), vendorService, catMap));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());

        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("vendor", vendor);
        model.addAttribute("lowStock", lowStock);

        model.addAttribute("categories", catMap);
        model.addAttribute("vendors", vendorService.getAllVendors());
        return "shop/shop";
    }

}
