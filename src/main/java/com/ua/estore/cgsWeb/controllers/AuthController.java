package com.ua.estore.cgsWeb.controllers;

import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.dto.ProductDTO;
import com.ua.estore.cgsWeb.services.CredentialService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;

@Slf4j
@Controller
@RequiredArgsConstructor
@SessionAttributes({"user"})
public class AuthController {

    private final CredentialService credentialService;


    /*************************************************************************************************************
     * Endpoints for standard login and logout
     ************************************************************************************************************/

    @GetMapping("/login")
    public String login() {
        return "main/login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session,
                              Model model) {

        log.info(String.format("Attempting to login user: %s", username));
        var userOpt = credentialService.authenticate(username, password);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(null);
            session.setAttribute("user", user);
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());
            session.setAttribute("cartItems", new ArrayList<ProductDTO>());
            log.info(String.format("User %s logged in successfully.", user.getUsername()));
            return "redirect:/";
        } else {
            log.error("Login Denied: Invalid username or password");
            model.addAttribute("error", "Invalid username or password");
            return "main/login";
        }
    }

    @GetMapping("/logout")
    public String logout(SessionStatus sessionStatus, RedirectAttributes redirectAttributes) {
        sessionStatus.setComplete();
        redirectAttributes.addFlashAttribute("message", "You have been logged out successfully!");
        return "redirect:/login";
    }

    /*************************************************************************************************************
     * Endpoints for new user registration
     ************************************************************************************************************/

    @GetMapping("/signup")
    public String register() {
        return "main/signup";
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

        //Password validation
        if (password.equals(confirmPassword)) {
            user.setPassword(password); //Encoded in service
        } else {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/signup";
        }

        user.setEmail((email == null || email.isBlank()) ? null : email.trim());
        user.setRole("USER");

        User.UserProfile profile = new User.UserProfile();
        profile.setFirstName(( firstName == null ? null : firstName.trim()));
        profile.setMiddleInit(( middleInit == null ? null : middleInit.trim()));
        profile.setLastName(( lastName == null ? null : lastName.trim()));
        profile.setPhoneNumber(( phone == null || phone.isBlank() ) ? null : phone.trim());
        user.setProfile(profile);

        log.info(String.format("Registering new user: { %s, %s, %s, %s, %s, %s, %s }", user.getUsername(), user.getRole(),
                user.getEmail(), user.getProfile().getPhoneNumber(), user.getProfile().getFirstName(),
                user.getProfile().getMiddleInit(), user.getProfile().getLastName()));
        try {
            String insertedId = credentialService.saveUser(user);

            if (insertedId == null || insertedId.isBlank()) {
                log.error("Failed to save user record.");
                throw new IllegalArgumentException("Failed to save user record.");
            }

            log.info("User record saved successfully with ID: " + insertedId);
            redirectAttributes.addFlashAttribute("message", "User Successfully Registered! Login now to view the shop!");
            return "redirect:/login";

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/signup";
        }
    }

}
