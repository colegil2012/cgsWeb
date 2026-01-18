package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.wrappers.ProductFormWrapper;
import com.ua.estore.cgsWeb.services.ProductService;
import com.ua.estore.cgsWeb.services.VendorService;
import com.ua.estore.cgsWeb.util.searchUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class VendorController {

    private final ProductService productService;
    private final VendorService vendorService;

    @Value("${app.upload.path}")
    private String uploadPath;

    /**********************************************************************************
     * Controller methods for handling vendor-related operations
     *********************************************************************************/

    @GetMapping("/vendor")
    public String vendorPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");

        // Check if user exists and is a vendor
        if (user == null || !"VENDOR".equalsIgnoreCase(user.getRole())) {
            return "redirect:/login";
        }

        List<String> categories = searchUtil.getCategories(productService.getAllProducts());

        model.addAttribute("categories", categories);
        model.addAttribute("vendorId", user.getVendorId());

        return "vendor";
    }

    @PostMapping("/vendor/add-products")
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

                            Path path = Paths.get(uploadPath + fileName);
                            Files.createDirectories(path.getParent());
                            Files.write(path, imageFile.getBytes());

                            product.setImageUrl("/images/" + fileName);
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

        return "redirect:/vendor";
    }
}
