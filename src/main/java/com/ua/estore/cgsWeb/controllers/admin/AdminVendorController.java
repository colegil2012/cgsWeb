package com.ua.estore.cgsWeb.controllers.admin;

import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.models.vendor.Vendor;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin vendor management — {@code /admin/vendors/**}.
 *
 * <p>Gated by the existing {@code /admin/**} → {@code hasRole('ADMIN')} rule
 * in SecurityConfig. The {@code /admin/vendors/users/search} JSON endpoint
 * is under the same gate.</p>
 */
@Slf4j
@Controller
@RequestMapping("/admin/vendors")
@RequiredArgsConstructor
public class AdminVendorController {

    private final AdminVendorService adminVendorService;

    /* ============================================================================
     * List
     * ============================================================================ */

    @GetMapping
    public String list(@RequestParam(value = "page", defaultValue = "0") int page,
                       Model model) {
        AdminVendorService.PagedVendorResult result = adminVendorService.list(page);

        model.addAttribute("activeSection", "vendors");
        model.addAttribute("vendors", result.items());
        model.addAttribute("page", result.page());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("totalElements", result.totalElements());
        return "admin/vendors/list";
    }

    /* ============================================================================
     * Create
     * ============================================================================ */

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("activeSection", "vendors");
        model.addAttribute("vendor", null);          // null => create mode
        return "admin/vendors/edit";
    }

    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String slug,
                         @RequestParam(required = false, defaultValue = "false") boolean active,
                         @RequestParam(required = false) String logoUrl,
                         RedirectAttributes redirectAttributes) {
        try {
            Vendor created = adminVendorService.createVendor(
                    name, description, slug, active, logoUrl);
            redirectAttributes.addFlashAttribute("message", "Vendor created.");
            return "redirect:/admin/vendors/" + created.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/admin/vendors/new";
        }
    }

    /* ============================================================================
     * Detail
     * ============================================================================ */

    @GetMapping("/{id}")
    public String detail(@PathVariable String id,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Vendor vendor = adminVendorService.findById(id).orElse(null);
        if (vendor == null) {
            redirectAttributes.addFlashAttribute("error", "Vendor not found.");
            return "redirect:/admin/vendors";
        }
        List<User> assignedUsers = adminVendorService.usersForVendor(id);

        model.addAttribute("activeSection", "vendors");
        model.addAttribute("vendor", vendor);
        model.addAttribute("assignedUsers", assignedUsers);
        model.addAttribute("productCount", adminVendorService.productCount(id));
        return "admin/vendors/detail";
    }

    /* ============================================================================
     * Edit
     * ============================================================================ */

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Vendor vendor = adminVendorService.findById(id).orElse(null);
        if (vendor == null) {
            redirectAttributes.addFlashAttribute("error", "Vendor not found.");
            return "redirect:/admin/vendors";
        }
        model.addAttribute("activeSection", "vendors");
        model.addAttribute("vendor", vendor);
        return "admin/vendors/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable String id,
                         @RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String slug,
                         @RequestParam(required = false, defaultValue = "false") boolean active,
                         @RequestParam(required = false) String logoUrl,
                         RedirectAttributes redirectAttributes) {
        try {
            adminVendorService.updateVendor(id, name, description, slug, active, logoUrl);
            redirectAttributes.addFlashAttribute("message", "Vendor updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/vendors/" + id;
    }

    /* ============================================================================
     * User assignment
     * ============================================================================ */

    /**
     * JSON search endpoint for the assign-user modal. Returns up to 10
     * matching users as {id, username, email, displayName, alreadyAssigned}.
     */
    @GetMapping("/users/search")
    @ResponseBody
    public List<AdminVendorService.UserSearchResult> searchUsers(
            @RequestParam("q") String query) {
        return adminVendorService.searchUsers(query);
    }

    @PostMapping("/{id}/assign-user")
    public String assignUser(@PathVariable String id,
                             @RequestParam String userId,
                             RedirectAttributes redirectAttributes) {
        try {
            adminVendorService.assignUserToVendor(userId, id);
            redirectAttributes.addFlashAttribute("message", "User assigned to vendor.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/vendors/" + id;
    }

    @PostMapping("/{id}/unassign-user")
    public String unassignUser(@PathVariable String id,
                               @RequestParam String userId,
                               RedirectAttributes redirectAttributes) {
        try {
            adminVendorService.unassignUserFromVendor(userId, id);
            redirectAttributes.addFlashAttribute("message", "User unassigned from vendor.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/vendors/" + id;
    }
}