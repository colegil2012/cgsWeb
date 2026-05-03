package com.ua.estore.cgsWeb.controllers.user;

import com.ua.estore.cgsWeb.models.shop.Order;
import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.models.wrappers.AddressUpdateWrapper;
import com.ua.estore.cgsWeb.services.address.AddressService;
import com.ua.estore.cgsWeb.services.shop.OrderService;
import com.ua.estore.cgsWeb.services.user.CredentialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.ua.estore.cgsWeb.util.requestUtil.getReferalUrl;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AccountController  {

    private final CredentialService credentialService;
    private final OrderService orderService;
    private final AddressService addressService;


    /**********************************************************************************
     * Controller methods for handling account-related operations
     *********************************************************************************/

    @GetMapping("/account")
    public String accountPage(HttpSession session,
                              Model model,
                              @RequestParam(name = "tab", required = false, defaultValue = "profile") String tab,
                              @RequestParam(name = "page", defaultValue = "0") int page) {

        User user = (User) session.getAttribute("user");
        if (user.getUsername() == null) return "redirect:/login";

        if (!tab.matches("profile|addresses|orders|security")) {
            tab = "profile";
        }
        model.addAttribute("activeTab", tab);

        //Refresh User from DB
        credentialService.getUserByUsername(user.getUsername()).ifPresent(vUser -> {
            model.addAttribute("user", vUser);
        });

        if ("orders".equals(tab)) {
            org.springframework.data.domain.Page<Order> orders =
                    orderService.findByUserPage(user.getId(), page);
            model.addAttribute("orders", orders);
            model.addAttribute("ordersPage", orders.getNumber());
            model.addAttribute("ordersTotalPages", orders.getTotalPages());
        }

        return "user/account";
    }


    /****************************************************************************************
     * Change Password
     ****************************************************************************************/

    @PostMapping("/account/password")
    public String changePassword(HttpSession session,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmNewPassword,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getUsername() == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Please login again to change your password");
            return "redirect:/login";
        }

        try {
            credentialService.updatePassword(user.getId(), oldPassword, newPassword, confirmNewPassword);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "Password updated successfully");
            return "redirect:/account?tab=security";

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage());
            return "redirect:/account?tab=security";

        } catch (Exception e) {
            log.error("Unexpected error while updating password for user={}", user.getUsername(), e);
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Unexpected error occurred while updating password");
            return "redirect:/account?tab=security";
        }
    }

    /*****************************************************************************
     * Update User Addresses
     ****************************************************************************/

    @PostMapping("/account/addresses")
    public String updateAddresses(HttpSession session,
                                  HttpServletRequest request,
                                  @ModelAttribute AddressUpdateWrapper form,
                                  RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("user");
        if (user == null || user.getUsername() == null) {
            redirectAttributes.addFlashAttribute("error", "Please login again to update addresses.");
            return "redirect:/login";
        }

        String returnTo = getReferalUrl(request.getHeader("Referer"), "/account?tab=addresses");

        try {
            log.info("Form Submission data={}", form.getNewAddresses());
            addressService.updateUserAddresses(user.getId(), form);
            log.info("Addresses updated successfully for user={}", user.getUsername());

            // Refresh session user so subsequent pages (like /cart) see updated addresses
            credentialService.getUserById(user.getId()).ifPresent(fresh -> {
                // remove password from session
                fresh.setPassword(null);
                session.setAttribute("user", fresh);
            });

            redirectAttributes.addFlashAttribute("message", "Addresses updated successfully.");
            return "redirect:" + returnTo;

        } catch (IllegalArgumentException ex) {
            log.error("Invalid address data provided for user={}", user.getUsername(), ex);
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:" + returnTo;

        } catch (Exception ex) {
            log.error("Unexpected error updating addresses for user={}", user.getUsername(), ex);
            redirectAttributes.addFlashAttribute("error", "Unexpected error occurred while updating addresses.");
            return "redirect:" + returnTo;
        }
    }
}
