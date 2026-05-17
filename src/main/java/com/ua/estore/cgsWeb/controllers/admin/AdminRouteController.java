package com.ua.estore.cgsWeb.controllers.admin;

import com.ua.estore.cgsWeb.services.admin.AdminRouteService;
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
 * Admin route browsing — {@code /admin/routes/**}. Read-only.
 */
@Slf4j
@Controller
@RequestMapping("/admin/routes")
@RequiredArgsConstructor
public class AdminRouteController {

    private final AdminRouteService adminRouteService;

    @GetMapping
    public String list(@RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {
        AdminRouteService.PagedRouteResult result =
                adminRouteService.list(status, page);

        model.addAttribute("activeSection", "routes");
        model.addAttribute("routes", result.items());
        model.addAttribute("page", result.page());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("totalElements", result.totalElements());
        model.addAttribute("statusFilter", result.statusFilter());
        return "admin/routes/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        AdminRouteService.RouteDetail detail =
                adminRouteService.findRouteDetail(id).orElse(null);
        if (detail == null) {
            redirectAttributes.addFlashAttribute("error", "Route not found.");
            return "redirect:/admin/routes";
        }

        model.addAttribute("activeSection", "routes");
        model.addAttribute("route", detail.route());
        model.addAttribute("stops", detail.stops());
        return "admin/routes/detail";
    }
}