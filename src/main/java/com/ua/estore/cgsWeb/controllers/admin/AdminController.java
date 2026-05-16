package com.ua.estore.cgsWeb.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Admin portal entry points.
 *
 * <p>Auth: {@code /admin/**} is gated to {@code hasRole('ADMIN')} in
 * {@link com.ua.estore.cgsWeb.config.SecurityConfig}. No additional checks
 * needed at the controller layer — by the time a request reaches us, the
 * caller has the admin role.</p>
 *
 * <p>This controller owns the dashboard and the "coming soon" placeholders
 * for Round 1B/2 tabs (Vendors, Routes, Orders). The Users tab has its own
 * controller — see {@link AdminUserController}.</p>
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("activeSection", "dashboard");
        return "admin/admin";
    }

    /* ============================================================================
     * Round 1B / Round 2 placeholders — render the coming-soon template so the
     * sidebar nav items don't 404 during the period after Round 1A ships but
     * before the rest does.
     * ============================================================================ */

    @GetMapping("/routes")
    public String routesPlaceholder(Model model) {
        model.addAttribute("activeSection", "routes");
        model.addAttribute("sectionTitle", "Routes");
        model.addAttribute("sectionDescription",
                "Browse delivery routes — planned, in progress, completed, and cancelled. Coming in Round 2.");
        return "admin/admin-coming-soon";
    }

    @GetMapping("/orders")
    public String ordersPlaceholder(Model model) {
        model.addAttribute("activeSection", "orders");
        model.addAttribute("sectionTitle", "Orders");
        model.addAttribute("sectionDescription",
                "Browse all orders with status filters and per-order detail. Coming in Round 2.");
        return "admin/admin-coming-soon";
    }
}