package com.ua.estore.cgsWeb.controllers.user;

import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.services.user.CredentialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@SessionAttributes({"user"})
public class UserController {

    private final CredentialService credentialService;

    /*************************************************************************************************************
     * Login view (Spring Security handles POST /login and /logout automatically)
     ************************************************************************************************************/

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully!");
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
                                  @RequestParam(required = false) String email,
                                  @RequestParam String username,
                                  @RequestParam String password,
                                  @RequestParam String confirmPassword,
                                  RedirectAttributes redirectAttributes) {

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

        user.setEmail((email == null || email.isBlank()) ? null : email.trim());
        user.setRoles(roles);

        User.UserProfile profile = new User.UserProfile();
        profile.setFirstName((firstName == null ? null : firstName.trim()));
        profile.setMiddleInit((middleInit == null ? null : middleInit.trim()));
        profile.setLastName((lastName == null ? null : lastName.trim()));
        profile.setPhoneNumber((phone == null || phone.isBlank()) ? null : phone.trim());
        user.setProfile(profile);

        log.info("Registering new user: {}", user.getUsername());
        try {
            String insertedId = credentialService.saveUser(user);

            if (insertedId == null || insertedId.isBlank()) {
                throw new IllegalArgumentException("Failed to save user record.");
            }

            log.info("User record saved successfully with ID: {}", insertedId);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "User Successfully Registered! Login now to view the shop!");
            return "redirect:/login";

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/signup";
        }
    }
}