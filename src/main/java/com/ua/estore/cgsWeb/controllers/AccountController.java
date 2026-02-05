package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.Order;
import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.wrappers.AddressUpdateWrapper;
import com.ua.estore.cgsWeb.services.*;
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
                              HttpServletRequest request,
                              @RequestParam(name = "tab", required = false, defaultValue = "profile") String tab,
                              @RequestParam(defaultValue = "0") int orderPage) {

        User user = (User) session.getAttribute("user");
        log.info("Accessing account page for user: {}", user.getUsername());

        if (user.getUsername() == null) return "redirect:/login";

        if (!tab.matches("profile|addresses|orders|security")) {
            tab = "profile";
        }

        model.addAttribute("activeTab", tab);

        credentialService.getUserByUsername(user.getUsername()).ifPresent(vUser -> {
            model.addAttribute("user", vUser);
        });

        Page<Order> orders = orderService.findByUserId(user.getId(), PageRequest.of(orderPage, 5));
        model.addAttribute("orders", orders);

        //Temporary logging for coding orders logic
        if (!orders.isEmpty()) {
            log.info("Found {} orders for user: {}", orders.getTotalElements(), user.getUsername());
        } else {
            log.info("No orders found for user: {}", user.getUsername());
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
            redirectAttributes.addFlashAttribute("pwErr", "Please login again to change your password");
            return "redirect:/login";
        }

        try {
            credentialService.updatePassword(user.getId(), oldPassword, newPassword, confirmNewPassword);
            log.info("Password updated successfully for user={}", user.getUsername());
            redirectAttributes.addFlashAttribute("pwMsg", "Password updated successfully");
            return "redirect:/account?tab=security";

        } catch (IllegalArgumentException ex) {
            log.error("Invalid password data provided for user={}", user.getUsername(), ex);
            redirectAttributes.addFlashAttribute("pwErr", ex.getMessage());
            return "redirect:/account?tab=security";

        } catch (Exception e) {
            log.error("Unexpected error while updating password for user={}", user.getUsername(), e);
            redirectAttributes.addFlashAttribute("pwErr", "Unexpected error occurred while updating password");
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
            redirectAttributes.addFlashAttribute("addrErr", "Please login again to update addresses.");
            return "redirect:/login";
        }

        String returnTo = getReferalUrl(request.getHeader("Referer"), "/account?tab=addresses");

        try {
            log.info("Form Submission data={}", form.getNewAddresses());
            addressService.updateUserAddresses(user.getId(), form);
            log.info("Addresses updated successfully for user={}", user.getUsername());

            // Refresh session user so subsequent pages (like /cart) see updated addresses
            credentialService.getUserById(user.getId()).ifPresent(fresh -> {
                fresh.setPassword(null); // avoid keeping password in session
                session.setAttribute("user", fresh);
            });

            redirectAttributes.addFlashAttribute("addrMsg", "Addresses updated successfully.");
            return "redirect:" + returnTo;

        } catch (IllegalArgumentException ex) {
            log.error("Invalid address data provided for user={}", user.getUsername(), ex);
            redirectAttributes.addFlashAttribute("addrErr", ex.getMessage());
            return "redirect:" + returnTo;

        } catch (Exception ex) {
            log.error("Unexpected error updating addresses for user={}", user.getUsername(), ex);
            redirectAttributes.addFlashAttribute("addrErr", "Unexpected error occurred while updating addresses.");
            return "redirect:" + returnTo;
        }
    }
}
