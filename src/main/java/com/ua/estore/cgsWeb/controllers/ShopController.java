package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.Vendor;
import com.ua.estore.cgsWeb.models.dto.ProductDTO;
import com.ua.estore.cgsWeb.services.ProductService;
import com.ua.estore.cgsWeb.services.VendorService;
import com.ua.estore.cgsWeb.util.dataUtil;
import com.ua.estore.cgsWeb.util.searchUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ShopController {

    private final ProductService productService;
    private final VendorService vendorService;

    /**********************************************************************************
     * Controller methods for handling shop-related operations
     *********************************************************************************/

    @GetMapping("/shop")
    public String shop(@RequestParam(defaultValue = "0") int page, Model model) {
        List<Product> products = productService.getAllProducts();
        List<String> categories = searchUtil.getCategories(products);
        List<Vendor> vendors = vendorService.getAllVendors();

        model.addAttribute("vendors", vendors);
        model.addAttribute("products", dataUtil.convertToProductDto(products, vendorService));
        model.addAttribute("categories", categories);

        return executeFiltering("", "", "", false, page, model);
    }

    @GetMapping("/shop/filter")
    public String filterProducts(@RequestParam(required = false) String search,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(name = "vendor", required = false) String vendor,
                                 @RequestParam(name = "lowStock", defaultValue = "false") boolean lowStock,
                                 @RequestParam(defaultValue = "0") int page,
                                 Model model) {

        return executeFiltering(search, category, vendor, lowStock, page, model);
    }

    @GetMapping("/shop/view/{id}")
    public String viewProduct(@PathVariable String id, Model model) {
        Product product = productService.getProductById(id);
        if( product == null ) return "redirect:/shop";

        List<ProductDTO> dtos = dataUtil.convertToProductDto(List.of(product), vendorService);
        if (dtos.isEmpty()) return "redirect:/shop";

        ProductDTO productDto = dtos.getFirst();

        model.addAttribute("selected_product", productDto);
        return "product";
    }



    /********************************************************************************
       Helper Methods
     *******************************************************************************/


    private String executeFiltering(String search, String category, String vendor, boolean lowStock, int page, Model model) {
        Page<Product> productPage = productService.getProductsByFilter(search, category, vendor, lowStock, page);

        model.addAttribute("products", dataUtil.convertToProductDto(productPage.getContent(), vendorService));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());

        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("vendor", vendor);
        model.addAttribute("lowStock", lowStock);

        model.addAttribute("categories", searchUtil.getCategories(productService.getAllProducts()));
        model.addAttribute("vendors", vendorService.getAllVendors());
        return "shop";
    }

}
