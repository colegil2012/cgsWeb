package com.ua.estore.cgsWeb.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class VendorController {

    @GetMapping("/vendor")
    public String vendorPage() {
        return "vendor";
    }
}
