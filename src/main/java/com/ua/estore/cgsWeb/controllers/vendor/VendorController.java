package com.ua.estore.cgsWeb.controllers.vendor;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.Vendor;
import com.ua.estore.cgsWeb.models.wrappers.AddressUpdateWrapper;
import com.ua.estore.cgsWeb.models.wrappers.ProductFormWrapper;
import com.ua.estore.cgsWeb.services.address.AddressService;
import com.ua.estore.cgsWeb.services.shop.CategoryService;
import com.ua.estore.cgsWeb.services.shop.ProductService;
import com.ua.estore.cgsWeb.services.vendor.VendorService;
import com.ua.estore.cgsWeb.services.storage.ImageStorageService;
import com.ua.estore.cgsWeb.util.dataUtil;
import com.ua.estore.cgsWeb.util.requestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ua.estore.cgsWeb.util.requestUtil.getReferalUrl;

@Slf4j
@Controller
@RequiredArgsConstructor
public class VendorController {

    private final ProductService productService;
    private final VendorService vendorService;
    private final CategoryService categoryService;
    private final ImageStorageService imageStorageService;
    private final AddressService addressService;


    /**********************************************************************************
     * Controller methods for handling vendor-portal related operations
     *********************************************************************************/

    @GetMapping("/vendor/portal")
    public String vendorPage(HttpSession session,
                             Model model,
                             @RequestParam(name= "tab", defaultValue = "profile") String tab) {

        User user = (User) session.getAttribute("user");
        if (user == null || !user.getRoles().contains("VENDOR")) { return "redirect:/login"; }

        if(user.getVendorId() != null) {
            vendorService.getVendorById(user.getVendorId()).ifPresent(vendor -> {
                model.addAttribute("vendorDetail", vendor);
            });
        }

        if (!tab.matches("profile|addresses|inventory|orders")) {
            tab = "profile";
        }

        model.addAttribute("activeTab", tab);
        model.addAttribute("products", productService.getProductsByVendorId(user.getVendorId()));
        model.addAttribute("categories", categoryService.getCategoryNameMap());

        return "vendor/vendor-portal";
    }

    /**********************************************************************************
     * UPLOAD VENDOR LOGO
     *********************************************************************************/

    @PostMapping("/vendor/portal/update-logo")
    public String uploadVendorLogo(HttpSession session,
                                              RedirectAttributes redirectAttributes,
                                              @RequestParam("vendorId") String vendorId,
                                              @RequestParam("vendorLogo") MultipartFile vendorLogo) {
        List<String> successes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !user.getRoles().contains("VENDOR")) {
                errors.add("User not authorized.");
                redirectAttributes.addFlashAttribute("message", successes);
                redirectAttributes.addFlashAttribute("error", errors);
                return "redirect:/vendor/portal";
            }

            if (user.getVendorId() == null || user.getVendorId().isBlank()) {
                errors.add("Vendor not found for user.");
                redirectAttributes.addFlashAttribute("message", successes);
                redirectAttributes.addFlashAttribute("error", errors);
                return "redirect:/vendor/portal";
            }

            if (vendorId == null || vendorId.isBlank()) {
                errors.add("Vendor id is missing.");
                redirectAttributes.addFlashAttribute("message", successes);
                redirectAttributes.addFlashAttribute("error", errors);
                return "redirect:/vendor/portal";
            }

            if (!vendorId.equals(user.getVendorId())) {
                errors.add("You are not allowed to update this vendor.");
                redirectAttributes.addFlashAttribute("message", successes);
                redirectAttributes.addFlashAttribute("error", errors);
                return "redirect:/vendor/portal";
            }

            if (vendorLogo == null || vendorLogo.isEmpty()) {
                errors.add("No file selected.");
                redirectAttributes.addFlashAttribute("message", successes);
                redirectAttributes.addFlashAttribute("error", errors);
                return "redirect:/vendor/portal";
            }

            Vendor vendor = vendorService.getVendorById(user.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found."));

            String safeOriginalName = vendorLogo.getOriginalFilename() == null ? "logo" : vendorLogo.getOriginalFilename();
            String fileName = System.currentTimeMillis() + "_" + safeOriginalName;

            imageStorageService.storeVendorLogo(fileName, vendorLogo);

            String logoUrl = "/images/vendors/" + fileName;
            vendorService.updateVendorLogoUrl(vendor.getId(), logoUrl);

            // Refresh vendor detail in session (optional)
            vendorService.getVendorById(user.getVendorId()).ifPresent(fresh -> session.setAttribute("vendorDetail", fresh));
            successes.add("Logo updated successfully.");

        } catch (IllegalArgumentException ex) {
            errors.add(ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error uploading vendor logo", ex);
            errors.add("Upload failed. Please try again.");
        }

        redirectAttributes.addFlashAttribute("message", successes);
        redirectAttributes.addFlashAttribute("error", errors);

        return "redirect:/vendor/portal";
    }


    /**********************************************************************************
     * Update Vendor Settings
     *********************************************************************************/

    @PostMapping("/vendor/portal/update-settings")
    public String updateVendorSettings(HttpSession session,
                                       RedirectAttributes redirectAttributes,
                                       @RequestParam("vendorId") String vendorId,
                                       @RequestParam("leadTime") int leadTime) {

        List<String> successes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !user.getRoles().contains("VENDOR")) {
                errors.add("User not authorized.");
            } else if (user.getVendorId() == null || user.getVendorId().isBlank()) {
                errors.add("Vendor not found for user.");
            } else if (vendorId == null || vendorId.isBlank()) {
                errors.add("Vendor id is missing.");
            } else if (!vendorId.equals(user.getVendorId())) {
                errors.add("You are not allowed to update this vendor.");
            } else if (leadTime < 0) {
                errors.add("Lead time cannot be negative.");
            } else {
                vendorService.updateSettings(user.getVendorId(), leadTime);

                // refresh vendor detail so UI reflects new values immediately
                vendorService.getVendorById(user.getVendorId())
                        .ifPresent(fresh -> session.setAttribute("vendorDetail", fresh));

                successes.add("Vendor settings updated successfully.");
            }
        } catch (Exception ex) {
            log.error("Unexpected error updating vendor settings", ex);
            errors.add("Failed to update settings. Please try again.");
        }

        redirectAttributes.addFlashAttribute("message", successes);
        redirectAttributes.addFlashAttribute("error", errors);

        return "redirect:/vendor/portal";
    }


    /**********************************************************************************
     * Add Products for a Vendor
     *********************************************************************************/

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

                            // Store bytes (LocalImageStorageService in dev, SpacesImageStorageService in prod)
                            imageStorageService.storeProductImage(fileName, imageFile);

                            // Store the same logical path in Mongo;
                            // prod base-url can resolve this to Spaces.
                            product.setImageUrl("/images/products/" + fileName);
                        }
                    }

                    String insertedId = productService.saveProduct(product, vendorId);
                    log.info("Product with id {} successfully added to vendor {}", insertedId, vendorId);
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

    /**********************************************************************************
     * Update Individual Product
     *********************************************************************************/

    @PostMapping("/vendor/portal/update-product")
    public String updateProduct(HttpSession session,
                                RedirectAttributes redirectAttributes,
                                @RequestParam("productId") String productId,
                                @RequestParam("name") String name,
                                @RequestParam("description") String description,
                                @RequestParam("price") BigDecimal price,
                                @RequestParam(value = "salePrice", required = false) BigDecimal salePrice,
                                @RequestParam("stock") int stock,
                                @RequestParam(value = "lowStockThreshold", required = false, defaultValue = "0") int lowStockThreshold,
                                @RequestParam(value = "active", required = false, defaultValue = "false") boolean active) {

        List<String> successes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !user.getRoles().contains("VENDOR")) {
                errors.add("User not authorized.");
            } else if (productId == null || productId.isBlank()) {
                errors.add("Product ID is missing.");
            } else {
                Product product = productService.getProductById(productId);
                if (product == null) {
                    errors.add("Product not found.");
                } else if (!product.getVendorId().equals(user.getVendorId())) {
                    errors.add("You are not authorized to update this product.");
                } else {
                    product.setName(name);
                    product.setDescription(description);
                    product.setPrice(price);
                    product.setSalePrice(salePrice);
                    product.setStock(stock);
                    product.setLowStockThreshold(lowStockThreshold);
                    product.setActive(active);

                    productService.saveProduct(product, user.getVendorId());
                    successes.add("Product '" + name + "' updated successfully.");
                }
            }
        } catch (Exception ex) {
            log.error("Unexpected error updating product", ex);
            errors.add("Failed to update product. Please try again.");
        }

        redirectAttributes.addFlashAttribute("message", successes);
        redirectAttributes.addFlashAttribute("error", errors);

        return "redirect:/vendor/portal?tab=inventory";
    }

    /**********************************************************************************
     * Update Vendor Addresses
     **********************************************************************************/

    @PostMapping("/vendor/addresses")
    public String updateAddresses(HttpSession session,
                                  HttpServletRequest request,
                                  @ModelAttribute AddressUpdateWrapper form,
                                  RedirectAttributes redirectAttributes) {

        List<String> successes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        User user = (User) session.getAttribute("user");
        if (user == null || user.getUsername() == null) {
            errors.add("Please login again to update addresses.");
            redirectAttributes.addFlashAttribute("message", successes);
            redirectAttributes.addFlashAttribute("error", errors);
            return "redirect:/login";
        }

        Optional<Vendor> vendor = vendorService.getVendorById(user.getVendorId());
        if(vendor.isEmpty() || vendor.get().getId() == null) {
            errors.add("Vendor not found.");
            redirectAttributes.addFlashAttribute("message", successes);
            redirectAttributes.addFlashAttribute("error", errors);
            return "redirect:/vendor/portal";
        }


        String returnTo = getReferalUrl(request.getHeader("Referer"), "/vendor/portal");

        try {
            log.info("Form Submission data={}", form.getNewAddresses());
            addressService.updateVendorAddresses(vendor.get().getId(), form);
            log.info("Addresses updated successfully for Vendor={}", vendor.get().getName());

            //Refresh Vendor Details so it sees updated addresses
            vendorService.getVendorById(user.getVendorId()).ifPresent(fresh -> {
                session.setAttribute("vendorDetail", fresh);
            });

            successes.add("Addresses updated successfully.");

        } catch (IllegalArgumentException ex) {
            log.error("Invalid address data provided for Vendor={}", user.getUsername(), ex);
            errors.add(ex.getMessage());

        } catch (Exception ex) {
            log.error("Unexpected error updating addresses for Vendor={}", user.getUsername(), ex);
            errors.add("Failed to update addresses. Please try again.");
        }

        redirectAttributes.addFlashAttribute("message", successes);
        redirectAttributes.addFlashAttribute("error", errors);

        return "redirect:" + returnTo;
    }

}
