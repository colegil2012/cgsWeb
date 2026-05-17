package com.ua.estore.cgsWeb.controllers.admin;

import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.services.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin user management routes — {@code /admin/users/**}.
 *
 * <p>The {@code user} session attribute (populated by
 * {@code AuthSuccessHandler}) is the admin doing the work. We pass their id
 * into the service so it can apply the self-action guards. The session
 * attribute is bound via {@code @SessionAttribute("user")} rather than
 * {@code @SessionAttributes}-class-level to keep this controller free of
 * cross-request model rebinding semantics — we just read it.</p>
 */
@Slf4j
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /* ============================================================================
     * List
     * ============================================================================ */

    @GetMapping
    public String list(@RequestParam(value = "role", required = false) String roleFilter,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @SessionAttribute(value = "user", required = false) User currentUser,
                       Model model) {

        String currentUserId = currentUser == null ? null : currentUser.getId();

        AdminUserService.PagedListResult result =
                adminUserService.list(roleFilter, page, currentUserId);

        model.addAttribute("activeSection", "users");
        model.addAttribute("users", result.items());
        model.addAttribute("page", result.page());
        model.addAttribute("totalPages", result.totalPages());
        model.addAttribute("totalElements", result.totalElements());
        model.addAttribute("roleFilter", result.roleFilter());   // null = "All"

        return "admin/users/list";
    }

    /* ============================================================================
     * Edit form
     * ============================================================================ */

    @GetMapping("/{id}")
    public String editForm(@PathVariable String id,
                           @SessionAttribute(value = "user", required = false) User currentUser,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        User target = adminUserService.findById(id).orElse(null);
        if (target == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }

        model.addAttribute("activeSection", "users");
        model.addAttribute("targetUser", target);
        model.addAttribute("isSelf",
                currentUser != null && currentUser.getId().equals(target.getId()));

        // Round 1A: VENDOR role can be REMOVED but not ADDED here (no vendor
        // picker yet). Pass a flag the template can use to disable the
        // checkbox when the user doesn't already have it.
        boolean canAssignVendor = target.getRoles() != null
                && target.getRoles().contains("VENDOR");
        model.addAttribute("canAssignVendor", canAssignVendor);

        return "admin/users/edit";
    }

    /* ============================================================================
     * Save profile + roles
     * ============================================================================ */

    @PostMapping("/{id}")
    public String save(@PathVariable String id,
                       @RequestParam(required = false) String firstName,
                       @RequestParam(required = false) String middleInit,
                       @RequestParam(required = false) String lastName,
                       @RequestParam(required = false) String phone,
                       @RequestParam(required = false) String email,
                       @RequestParam(value = "roles", required = false) List<String> roles,
                       @SessionAttribute(value = "user", required = false) User currentUser,
                       RedirectAttributes redirectAttributes) {

        String currentUserId = currentUser == null ? null : currentUser.getId();
        try {
            adminUserService.updateUser(
                    id, currentUserId,
                    firstName, middleInit, lastName, phone, email,
                    roles == null ? List.of() : roles);
            redirectAttributes.addFlashAttribute("message", "User updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    /* ============================================================================
     * Enable / disable
     * ============================================================================ */

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable String id,
                          @SessionAttribute(value = "user", required = false) User currentUser,
                          RedirectAttributes redirectAttributes) {
        String currentUserId = currentUser == null ? null : currentUser.getId();
        try {
            adminUserService.disableUser(id, currentUserId);
            redirectAttributes.addFlashAttribute("message", "Account disabled.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/enable")
    public String enable(@PathVariable String id,
                         @SessionAttribute(value = "user", required = false) User currentUser,
                         RedirectAttributes redirectAttributes) {
        String currentUserId = currentUser == null ? null : currentUser.getId();
        try {
            adminUserService.enableUser(id, currentUserId);
            redirectAttributes.addFlashAttribute("message", "Account re-enabled.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }

    /* ============================================================================
     * Password reset trigger
     * ============================================================================ */

    @PostMapping("/{id}/send-reset")
    public String sendReset(@PathVariable String id,
                            @SessionAttribute(value = "user", required = false) User currentUser,
                            RedirectAttributes redirectAttributes) {
        String currentUserId = currentUser == null ? null : currentUser.getId();
        try {
            adminUserService.sendPasswordResetEmail(id, currentUserId);
            redirectAttributes.addFlashAttribute("message",
                    "Password reset email sent. The user will receive a link to choose a new password.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users/" + id;
    }
}