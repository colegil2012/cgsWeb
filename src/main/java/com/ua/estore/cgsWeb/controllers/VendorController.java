package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.Vendor;
import com.ua.estore.cgsWeb.models.wrappers.ProductFormWrapper;
import com.ua.estore.cgsWeb.services.ProductService;
import com.ua.estore.cgsWeb.services.VendorService;
import com.ua.estore.cgsWeb.util.searchUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class VendorController {

    private final ProductService productService;
    private final VendorService vendorService;

    @Value("${app.upload.path}")
    private String uploadPath;

    /*********************************************************************************
     * View Vendor endpoints
     *********************************************************************************/

    @GetMapping("/vendors")
    public String allVendors(@RequestParam(defaultValue = "0") int page,
                             Model model) {
        Page<Vendor> vendorPage = vendorService.getAllVendors(page);

        model.addAttribute("vendors", vendorPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", vendorPage.getTotalPages());
        return "shop/view-all-vendors";
    }

    @GetMapping("/vendor/{id}")
    public String viewVendor(@RequestParam(defaultValue = "0") int page,
                             @PathVariable String id,
                             Model model) {
        Optional<Vendor> selectVendor = vendorService.getVendorById(id);

        if (selectVendor.isPresent()) {
            Page<Product> productPage = productService.getProductsByVendorId(id, page);

            model.addAttribute("vendor", selectVendor.get());
            model.addAttribute("products", productPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", productPage.getTotalPages());
            return "shop/view-vendor";
        }

        return "redirect:/vendors";
    }



    /**********************************************************************************
     * Controller methods for handling vendor-portal related operations
     *********************************************************************************/

    @GetMapping("/vendor/portal")
    public String vendorPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");

        // Check if user exists and is a vendor
        if (user == null || !"VENDOR".equalsIgnoreCase(user.getRole())) {
            return "redirect:/login";
        }

        List<String> categories = searchUtil.getCategories(productService.getAllProducts());

        if(user.getVendorId() != null) {
            vendorService.getVendorById(user.getVendorId()).ifPresent(vendor -> {
                model.addAttribute("vendorDetail", vendor);
            });
        }

        model.addAttribute("categories", categories);

        return "vendor/vendor-portal";
    }

    @PostMapping("/vendor/portal/add-products")
    public String addProduct(@ModelAttribute ProductFormWrapper form,
                             @RequestParam String vendorId,
                             RedirectAttributes redirectAttributes) {

        List<String> successes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (form.getProducts() != null) {
            for (int i = 0; i < form.getProducts().size(); i++) {
                Product product = form.getProducts().get(i);
                try {
                    // Handle Image Upload
                    if (form.getProductImages() != null && i < form.getProductImages().size()) {
                        var imageFile = form.getProductImages().get(i);
                        if (!imageFile.isEmpty()) {
                            String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();

                            Path path = Paths.get(uploadPath, "products", fileName);
                            Files.createDirectories(path.getParent());
                            Files.write(path, imageFile.getBytes());

                            product.setImageUrl("/images/products/" + fileName);
                        }
                    }

                    productService.saveProduct(product, vendorId);
                    successes.add("Successfully listed: " + product.getName());
                } catch (Exception e) {
                    errors.add("Failed to list " + product.getName() + ": " + e.getMessage());
                }
            }
        }

        redirectAttributes.addFlashAttribute("successMessages", successes);
        redirectAttributes.addFlashAttribute("errorMessages", errors);

        return "redirect:/vendor/portal";
    }
}
