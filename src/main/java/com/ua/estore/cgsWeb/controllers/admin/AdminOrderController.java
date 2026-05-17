package com.ua.estore.cgsWeb.controllers.admin;

import com.ua.estore.cgsWeb.services.admin.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin order browsing — {@code /admin/orders/**}. Read-only.
 *
 * <p>Gated by the existing {@code /admin/**} → {@code hasRole('ADMIN')} rule.</p>
 */
@Slf4j
@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public String list(@RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "q", required = false) String search,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {
        AdminOrderService.PagedOrderResult result =
                adminOrderService.list(status, search, page);

        model.addAttribute("activeSection", "orders");
        model.addAttribute("orders", result.items());
        model.addAttribute("page", result.page());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("totalElements", result.totalElements());
        model.addAttribute("statusFilter", result.statusFilter());
        model.addAttribute("search", result.search());
        return "admin/orders/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        AdminOrderService.OrderDetail detail =
                adminOrderService.findOrderDetail(id).orElse(null);
        if (detail == null) {
            redirectAttributes.addFlashAttribute("error", "Order not found.");
            return "redirect:/admin/orders";
        }

        model.addAttribute("activeSection", "orders");
        model.addAttribute("order", detail.order());
        model.addAttribute("delivery", detail.delivery());   // may be null
        model.addAttribute("route", detail.route());         // may be null
        return "admin/orders/detail";
    }
}