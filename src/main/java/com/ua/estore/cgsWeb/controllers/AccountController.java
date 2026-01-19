package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.services.CredentialService;
import com.ua.estore.cgsWeb.services.ProductService;
import com.ua.estore.cgsWeb.services.VendorService;
import com.ua.estore.cgsWeb.util.dataUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AccountController  {

    private final CredentialService credentialService;
    private final ProductService productService;
    private final VendorService vendorService;


    /**********************************************************************************
     * Controller methods for handling account-related operations
     *********************************************************************************/

    @GetMapping("/account")
    public String accountPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        log.info("Accessing account page for user: {}", user.getUsername());

        if (user.getUsername() == null) return "redirect:/login";

        credentialService.getUserByUsername(user.getUsername()).ifPresent(vUser -> {
            model.addAttribute("user", vUser);

            if ("VENDOR".equalsIgnoreCase(user.getRole()) && user.getVendorId() != null) {
                try {
                    // 1. Convert the String ID from the DB into a proper BSON ObjectId
                    ObjectId vendorObjectId = dataUtil.parseToObjectId(user.getVendorId());
                    log.info("Converted VendorID to ObjectId: {}", vendorObjectId);

                    vendorService.getVendorById(user.getVendorId()).ifPresent(vendor -> {
                        model.addAttribute("vendorInfo", vendor);
                    });

                    // 3. Filter products using ObjectId comparison for better reliability
                    List<Product> vendorProducts = productService.getAllProducts().stream()
                            .filter(p -> {
                                try {
                                    return vendorObjectId.equals(dataUtil.parseToObjectId(p.getVendorId()));
                                } catch (Exception e) {
                                    return false;
                                }
                            })
                            .toList();

                    log.info("Found {} products for vendor: {}", vendorProducts.size(), vendorObjectId);
                    model.addAttribute("vendorProducts", vendorProducts);

                } catch (IllegalArgumentException e) {
                    log.error("Failed to parse VendorID '{}' into a valid ObjectId: {}", user.getVendorId(), e.getMessage());
                }
            }
        });

        return "main/account";
    }
}
