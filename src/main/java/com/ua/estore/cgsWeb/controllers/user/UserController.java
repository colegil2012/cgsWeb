package com.ua.estore.cgsWeb.controllers.user;

import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.services.shop.CartService;
import com.ua.estore.cgsWeb.services.user.CredentialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@SessionAttributes({"user"})
public class UserController {

    private final CredentialService credentialService;
    private final CartService cartService;


    /*************************************************************************************************************
     * Endpoints for standard login and logout
     ************************************************************************************************************/

    @GetMapping("/login")
    public String login() {
        return "user/login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              HttpServletRequest request,
                              Model model) {

        var userOpt = credentialService.authenticate(username, password);

        if (userOpt.isPresent()) {
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);

            User user = userOpt.get();
            user.setPassword(null);
            session.setAttribute("user", user);

            var cart = cartService.getOrCreateByUserId(user.getId());
            session.setAttribute("userCart", cart);
            session.setAttribute("cartCount", cart.totalQuantity());

            log.info(String.format("User %s logged in.", user.getUsername()));
            return "redirect:/";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "user/login";
        }
    }

    @GetMapping("/logout")
    public String logout(SessionStatus sessionStatus,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        sessionStatus.setComplete();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        redirectAttributes.addFlashAttribute(
                "message",
                "You have been logged out successfully!");
        return "redirect:/login";
    }



    /*************************************************************************************************************
     * Endpoints for new user registration
     ************************************************************************************************************/

    @GetMapping("/signup")
    public String register() {
        return "user/signup";
    }


    /*************************************************************************************************************
     * Check Username Availability Endpoint
     ************************************************************************************************************/

    @PostMapping("/signup/checkUsername")
    public @ResponseBody Boolean checkUsername(@RequestParam String username) {
        return credentialService.checkUsername(username);
    }

    /*************************************************************************************************************
     * Submit New User Registration Endpoint
     ************************************************************************************************************/

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

        //Standard USER Role
        List<String> roles = new ArrayList<>();
        roles.add("USER");

        //Password validation
        if (password.equals(confirmPassword)) {
            user.setPassword(password); //Encoded in service
        } else {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Passwords do not match");
            return "redirect:/signup";
        }

        user.setEmail((email == null || email.isBlank()) ? null : email.trim());
        user.setRoles(roles);

        User.UserProfile profile = new User.UserProfile();
        profile.setFirstName(( firstName == null ? null : firstName.trim()));
        profile.setMiddleInit(( middleInit == null ? null : middleInit.trim()));
        profile.setLastName(( lastName == null ? null : lastName.trim()));
        profile.setPhoneNumber(( phone == null || phone.isBlank() ) ? null : phone.trim());
        user.setProfile(profile);

        log.info(String.format("Registering new user: { %s, %s, %s, %s, %s, %s, %s }",
                user.getUsername(), user.getRoles(), user.getEmail(), user.getProfile().getPhoneNumber(),
                user.getProfile().getFirstName(), user.getProfile().getMiddleInit(),
                user.getProfile().getLastName()));
        try {
            String insertedId = credentialService.saveUser(user);

            if (insertedId == null || insertedId.isBlank()) {
                throw new IllegalArgumentException("Failed to save user record.");
            }

            log.info("User record saved successfully with ID: " + insertedId);
            redirectAttributes.addFlashAttribute(
                    "message",
                    "User Successfully Registered! Login now to view the shop!");
            return "redirect:/login";

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage());
            return "redirect:/signup";
        }
    }

}
