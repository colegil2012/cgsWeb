package com.ua.estore.cgsWeb.controllers.admin;

import com.ua.estore.cgsWeb.models.shop.Product;
import com.ua.estore.cgsWeb.models.vendor.Vendor;
import com.ua.estore.cgsWeb.services.admin.AdminProductService;
import com.ua.estore.cgsWeb.services.admin.AdminVendorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Admin product management — {@code /admin/products/**}.
 *
 * <p>Products list globally or scoped to one vendor via {@code ?vendor=<id>}.
 * The vendor detail page links here with that param to show just that
 * vendor's catalog.</p>
 *
 * <p>The create/edit forms need the full vendor list for the vendor
 * dropdown — pulled via {@link AdminVendorService}.</p>
 */
@Slf4j
@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final AdminVendorService adminVendorService;

    /* ============================================================================
     * List
     * ============================================================================ */

    @GetMapping
    public String list(@RequestParam(value = "vendor", required = false) String vendorId,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {
        AdminProductService.PagedProductResult result =
                adminProductService.list(vendorId, page);

        model.addAttribute("activeSection", "products");
        model.addAttribute("products", result.items());
        model.addAttribute("page", result.page());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("totalElements", result.totalElements());
        model.addAttribute("vendorFilter", result.vendorFilter());

        // Vendor dropdown for the filter bar. Page 0, large size — vendor
        // count is small. If vendors ever exceed this, swap to a search.
        List<Vendor> allVendors = adminVendorService.list(0).items().stream()
                .map(v -> {
                    Vendor stub = new Vendor();
                    stub.setId(v.getId());
                    stub.setName(v.getName());
                    return stub;
                })
                .toList();
        model.addAttribute("allVendors", allVendors);

        // If scoped to a vendor, surface its name for the page header.
        if (result.vendorFilter() != null) {
            adminVendorService.findById(result.vendorFilter())
                    .ifPresent(v -> model.addAttribute("filterVendorName", v.getName()));
        }

        return "admin/products/list";
    }

    /* ============================================================================
     * Create
     * ============================================================================ */

    @GetMapping("/new")
    public String createForm(@RequestParam(value = "vendor", required = false) String vendorId,
                             Model model) {
        model.addAttribute("activeSection", "products");
        model.addAttribute("product", null);                  // null => create mode
        model.addAttribute("preselectedVendorId", vendorId);   // may be null
        model.addAttribute("allVendors", vendorStubs());
        return "admin/products/edit";
    }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String sku,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) BigDecimal price,
                         @RequestParam(required = false) BigDecimal salePrice,
                         @RequestParam(required = false) Integer stock,
                         @RequestParam(required = false) Integer lowStockThreshold,
                         @RequestParam(required = false) String imageUrl,
                         @RequestParam(required = false, defaultValue = "false") boolean active,
                         @RequestParam String vendorId,
                         @RequestParam(required = false) String slug,
                         @RequestParam(required = false) String categoryId,
                         RedirectAttributes redirectAttributes) {
        try {
            Product created = adminProductService.createProduct(
                    name, sku, description, price, salePrice, stock,
                    lowStockThreshold, imageUrl, active, vendorId, slug, categoryId);
            redirectAttributes.addFlashAttribute("message", "Product created.");
            return "redirect:/admin/products/" + created.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/products/new"
                    + (vendorId != null ? "?vendor=" + vendorId : "");
        }
    }

    /* ============================================================================
     * Edit
     * ============================================================================ */

    @GetMapping("/{id}")
    public String editForm(@PathVariable String id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Product product = adminProductService.findById(id).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Product not found.");
            return "redirect:/admin/products";
        }
        model.addAttribute("activeSection", "products");
        model.addAttribute("product", product);
        model.addAttribute("allVendors", vendorStubs());
        return "admin/products/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable String id,
                         @RequestParam String name,
                         @RequestParam(required = false) String sku,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) BigDecimal price,
                         @RequestParam(required = false) BigDecimal salePrice,
                         @RequestParam(required = false) Integer stock,
                         @RequestParam(required = false) Integer lowStockThreshold,
                         @RequestParam(required = false) String imageUrl,
                         @RequestParam(required = false, defaultValue = "false") boolean active,
                         @RequestParam(required = false) String slug,
                         @RequestParam(required = false) String categoryId,
                         RedirectAttributes redirectAttributes) {
        try {
            adminProductService.updateProduct(
                    id, name, sku, description, price, salePrice, stock,
                    lowStockThreshold, imageUrl, active, slug, categoryId);
            redirectAttributes.addFlashAttribute("message", "Product updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/products/" + id;
    }

    /* ============================================================================
     * Set stock — the dedicated restock action
     * ============================================================================ */

    @PostMapping("/{id}/set-stock")
    public String setStock(@PathVariable String id,
                           @RequestParam int stock,
                           @RequestParam(value = "returnTo", required = false) String returnTo,
                           RedirectAttributes redirectAttributes) {
        try {
            adminProductService.setStock(id, stock);
            redirectAttributes.addFlashAttribute("message", "Stock updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        // returnTo lets the list-view inline stock form come back to the list
        // (optionally still filtered by vendor) instead of the product page.
        if (returnTo != null && returnTo.startsWith("/admin/products")) {
            return "redirect:" + returnTo;
        }
        return "redirect:/admin/products/" + id;
    }

    /* ============================================================================
     * Helpers
     * ============================================================================ */

    /** Lightweight vendor list (id + name only) for form dropdowns. */
    private List<Vendor> vendorStubs() {
        return adminVendorService.list(0).items().stream()
                .map(v -> {
                    Vendor stub = new Vendor();
                    stub.setId(v.getId());
                    stub.setName(v.getName());
                    return stub;
                })
                .toList();
    }
}