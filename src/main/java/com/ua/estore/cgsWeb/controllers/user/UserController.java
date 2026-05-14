package com.ua.estore.cgsWeb.controllers.user;

import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.services.user.AccountService;
import com.ua.estore.cgsWeb.services.user.CredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Controller
@RequiredArgsConstructor
@SessionAttributes({"user"})
public class UserController {

    private final CredentialService credentialService;
    private final AccountService accountService;

    /**
     * Pragmatic email shape check — not RFC 5322-complete (nothing sane is),
     * just enough to catch typos and obviously-bad input before we try to
     * send mail to it. The real proof the address works is the user clicking
     * the verification link.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /*************************************************************************************************************
     * Login view (Spring Security handles POST /login and /logout automatically)
     ************************************************************************************************************/

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "verified", required = false) String verified,
                        Model model) {
        if (error != null) {
            // The failure handler routes unverified-account logins here with
            // error=unverified; everything else lands here with a bare error.
            if ("unverified".equals(error)) {
                model.addAttribute("error",
                        "Your email isn't verified yet. Check your inbox for the "
                                + "confirmation link — or request a new one below.");
                model.addAttribute("showResendVerification", true);
            } else {
                model.addAttribute("error", "Invalid username or password.");
            }
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully!");
        }
        if (verified != null) {
            // Set by AccountController after a successful email confirmation.
            model.addAttribute("message",
                    "Your email is verified — you can log in now.");
        }
        return "user/login";
    }

    /*************************************************************************************************************
     * Endpoints for new user registration
     ************************************************************************************************************/

    @GetMapping("/signup")
    public String register() {
        return "user/signup";
    }

    @PostMapping("/signup/checkUsername")
    public @ResponseBody Boolean checkUsername(@RequestParam String username) {
        // Returns true when the username is TAKEN (existing signup.js relies on this).
        return credentialService.checkUsername(username);
    }

    @PostMapping("/signup/submit")
    public String registerNewUser(@RequestParam String firstName,
                                  @RequestParam(required = false) String middleInit,
                                  @RequestParam String lastName,
                                  @RequestParam(required = false) String phone,
                                  @RequestParam String email,           // now REQUIRED
                                  @RequestParam String username,
                                  @RequestParam String password,
                                  @RequestParam String confirmPassword,
                                  RedirectAttributes redirectAttributes) {

        // ---- Email is now mandatory: the account can't be verified (and so
        // ---- can't ever log in) without a deliverable address. Validate
        // ---- presence + basic shape before doing any work.
        String normalizedEmail = (email == null) ? "" : email.trim();
        if (normalizedEmail.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email is required.");
            return "redirect:/signup";
        }
        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid email address.");
            return "redirect:/signup";
        }

        User user = new User();
        user.setUsername(username == null ? null : username.trim());

        List<String> roles = new ArrayList<>();
        roles.add("USER");

        if (password.equals(confirmPassword)) {
            user.setPassword(password);
        } else {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/signup";
        }

        user.setEmail(normalizedEmail);
        user.setRoles(roles);

        User.UserProfile profile = new User.UserProfile();
        profile.setFirstName((firstName == null ? null : firstName.trim()));
        profile.setMiddleInit((middleInit == null ? null : middleInit.trim()));
        profile.setLastName((lastName == null ? null : lastName.trim()));
        profile.setPhoneNumber((phone == null || phone.isBlank()) ? null : phone.trim());
        user.setProfile(profile);

        log.info("Registering new user: {}", user.getUsername());
        try {
            // saveUser now also sets enabled=false / emailVerified=false.
            String insertedId = credentialService.saveUser(user);

            if (insertedId == null || insertedId.isBlank()) {
                throw new IllegalArgumentException("Failed to save user record.");
            }
            log.info("User record saved successfully with ID: {}", insertedId);

            // saveUser mutated `user` in place (encoded password, set id +
            // flags), so it already has everything the mailer needs — id,
            // email, profile.firstName. No reload required.
            accountService.sendVerificationEmail(user);

            redirectAttributes.addFlashAttribute(
                    "message",
                    "Almost there! We've sent a confirmation link to "
                            + normalizedEmail
                            + ". Click it to activate your account, then log in.");
            return "redirect:/login";

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/signup";
        }
    }
}