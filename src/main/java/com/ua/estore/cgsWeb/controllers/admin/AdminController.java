package com.ua.estore.cgsWeb.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Admin portal entry point — just the dashboard.
 *
 * <p>Auth: {@code /admin/**} is gated to {@code hasRole('ADMIN')} in
 * {@link com.ua.estore.cgsWeb.config.SecurityConfig}.</p>
 *
 * <p>Every admin section now has its own controller:</p>
 * <ul>
 *   <li>Users    → {@link AdminUserController} (Round 1A)</li>
 *   <li>Vendors  → {@link AdminVendorController} (Round 1B)</li>
 *   <li>Products → {@link AdminProductController} (Round 1B)</li>
 *   <li>Routes   → {@link AdminRouteController} (Round 2)</li>
 *   <li>Orders   → {@link AdminOrderController} (Round 2)</li>
 * </ul>
 *
 * <p>No more placeholder mappings — every section is real. (The placeholders
 * that used to live here were removed as each section shipped; a placeholder
 * mapping the same path as a real controller is an ambiguous-mapping startup
 * failure.)</p>
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("activeSection", "dashboard");
        return "admin/admin";
    }
}