package com.ua.estore.cgsWeb.controllers.user;

import com.ua.estore.cgsWeb.models.shop.Order;
import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.models.wrappers.AddressUpdateWrapper;
import com.ua.estore.cgsWeb.services.address.AddressService;
import com.ua.estore.cgsWeb.services.shop.OrderService;
import com.ua.estore.cgsWeb.services.user.AccountService;
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

    private final AccountService accountService;
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

    /* ========================================================================
     * Email verification
     * ===================================================================== */

    /**
     * Land here from the link in the signup email. Consume the token; on
     * success the account is enabled and we bounce to the login page with a
     * success flag. On failure (expired / already-used / bogus token) we show
     * a page offering to re-send.
     */
    @GetMapping("/account/verify")
    public String verifyEmail(@RequestParam(value = "token", required = false) String token,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        boolean ok = accountService.confirmEmailVerification(token);

        if (ok) {
            // UserController.login() reads ?verified and shows a success message.
            return "redirect:/login?verified";
        }

        // Token bad/expired/used. Render a small page that explains and offers
        // the resend form. We don't say WHICH failure — consistent with the
        // opaque-failure contract in TokenService.
        model.addAttribute("reason",
                "This verification link is invalid or has expired.");
        return "account/verify-failed";
    }

    /**
     * Re-send a verification email. Offered on the verify-failed page and on
     * the login page when an unverified user tries to log in.
     *
     * <p>Same enumeration-oracle shape as password-reset: this always shows
     * the same confirmation regardless of whether the email maps to a real,
     * still-unverified account. The actual work runs on
     * {@code AccountService.resendVerificationEmail}, which is {@code @Async}
     * — the lookup and the issue/send both happen on a background thread, so
     * response timing doesn't leak whether the address mapped to an
     * unverified account.</p>
     */
    @PostMapping("/account/resend-verification")
    public String resendVerification(@RequestParam(value = "email", required = false) String email,
                                     RedirectAttributes redirectAttributes) {
        // Delegated to the service. The service decides whether the email
        // maps to an unverified account and acts (or doesn't) accordingly —
        // the controller never learns the outcome.
        accountService.resendVerificationEmail(email);

        redirectAttributes.addFlashAttribute("message",
                "If that email belongs to an unverified account, a new "
                        + "confirmation link is on its way.");
        return "redirect:/login";
    }

    /* ========================================================================
     * Password reset — request
     * ===================================================================== */

    /** The "enter your email to reset" form. */
    @GetMapping("/account/forgot-password")
    public String forgotPasswordForm() {
        return "account/forgot-password";
    }

    /**
     * Fire the reset email. This endpoint is enumeration-oracle-safe:
     *
     * <ul>
     *   <li>It returns the <em>same</em> message whether or not the email
     *       maps to a real account.</li>
     *   <li>It does <em>no conditional work</em> on the request thread —
     *       {@link AccountService#sendPasswordResetEmail} is {@code @Async},
     *       so the lookup + token-issue + render + send all happen on a
     *       background thread. Response timing is identical either way.</li>
     * </ul>
     *
     * The controller's job is just: validate the email is present, hand it
     * off, return the generic message.
     */
    @PostMapping("/account/request-reset")
    public String requestReset(@RequestParam(value = "email", required = false) String email,
                               RedirectAttributes redirectAttributes) {

        // Presence check only. We do NOT verify the email exists — that's the
        // whole point. An empty submission just goes back to the form.
        if (email == null || email.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Please enter your email address.");
            return "redirect:/account/forgot-password";
        }

        // Fire-and-forget. Returns immediately; all real work is on a
        // background thread. The controller never learns whether a user
        // was found.
        accountService.sendPasswordResetEmail(email);

        // The same response, every time.
        redirectAttributes.addFlashAttribute("message",
                "If an account exists for that email, a password-reset link "
                        + "is on its way. The link expires in 30 minutes.");
        return "redirect:/account/forgot-password";
    }

    /* ========================================================================
     * Password reset — perform
     * ===================================================================== */

    /**
     * The "choose a new password" form. We {@code peek} the token (validate
     * without consuming) so that merely opening the page doesn't burn the
     * link — the token is only spent on the POST. An invalid/expired token
     * gets the failure page instead of the form.
     */
    @GetMapping("/account/reset-password")
    public String resetPasswordForm(@RequestParam(value = "token", required = false) String token,
                                    Model model) {
        if (token == null || token.isBlank() || !accountService.isResetTokenValid(token)) {
            model.addAttribute("reason",
                    "This password-reset link is invalid or has expired.");
            return "account/reset-failed";
        }
        // Token is good — hand it to the form as a hidden field so the POST
        // can present it back.
        model.addAttribute("token", token);
        return "account/reset-password";
    }

    /**
     * Apply the new password. Consumes the token (single-use — see
     * TokenService) and updates the password via CredentialService.
     *
     * <p>Two distinct failure modes, handled separately:</p>
     * <ul>
     *   <li><b>Bad token</b> — {@code performPasswordReset} returns false.
     *       The link was invalid/expired/already-used. Show the failure
     *       page; there's no point re-rendering the form because the token
     *       is dead.</li>
     *   <li><b>Bad password</b> — token was fine and is now consumed, but
     *       the new password failed validation (too short, mismatch, same as
     *       old). {@code performPasswordReset} throws
     *       {@code IllegalArgumentException}. The token is already spent, so
     *       we can't just re-show the form with the same token — we send the
     *       user back to request a fresh link, with the specific error
     *       message so they know what went wrong.</li>
     * </ul>
     */
    @PostMapping("/account/reset-password")
    public String performReset(@RequestParam(value = "token", required = false) String token,
                               @RequestParam(value = "newPassword", required = false) String newPassword,
                               @RequestParam(value = "confirmNewPassword", required = false) String confirmNewPassword,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        try {
            boolean ok = accountService.performPasswordReset(token, newPassword, confirmNewPassword);

            if (!ok) {
                // Bad/expired/used token.
                model.addAttribute("reason",
                        "This password-reset link is invalid or has expired. "
                                + "Request a new one to continue.");
                return "account/reset-failed";
            }

            // Success — password changed. Bounce to login.
            redirectAttributes.addFlashAttribute("message",
                    "Your password has been reset. You can log in now.");
            return "redirect:/login";

        } catch (IllegalArgumentException ex) {
            // Token was valid and is now CONSUMED, but the password failed
            // validation. We can't re-show the form (dead token), so send
            // them to request a fresh link, carrying the reason.
            redirectAttributes.addFlashAttribute("error",
                    ex.getMessage()
                            + " Please request a new reset link and try again.");
            return "redirect:/account/forgot-password";
        }
    }

}
