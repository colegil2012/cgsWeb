package com.ua.estore.cgsWeb.services;

import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.models.dto.AddressDTO;
import com.ua.estore.cgsWeb.models.dto.ValidatedAddress;
import com.ua.estore.cgsWeb.models.wrappers.AddressUpdateWrapper;
import com.ua.estore.cgsWeb.repositories.UserRepository;
import com.ua.estore.cgsWeb.services.maps.GoogleAddressValidationService;
import com.ua.estore.cgsWeb.services.maps.ServiceAreaValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CredentialService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleAddressValidationService googleAddressValidationService;
    private final ServiceAreaValidationService serviceAreaValidationService;

    private static final Set<String> ALLOWED_ADDRESS_TYPES = Set.of("SHIPPING", "BILLING", "ALTERNATE");

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /********** Authenticate ***************/

    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return userOpt;
        }
        return Optional.empty();
    }

    /********** Save User ***************/

    public String saveUser(User user) {
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        String normalizedUsername = user.getUsername().trim();
        user.setUsername(normalizedUsername);

        // Check if username exists in db
        if (getUserByUsername(normalizedUsername).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepository.save(user);
        return user.getId();
    }

    /********** Update Password ***************/

    public void updatePassword(String userId, String oldPassword, String newPassword, String confirmNewPassword) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User not found.");
        }
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new IllegalArgumentException("Old password is required.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required.");
        }
        if (confirmNewPassword == null || confirmNewPassword.isBlank()) {
            throw new IllegalArgumentException("Confirm password is required.");
        }
        if (newPassword.length() < 10) {
            throw new IllegalArgumentException("Password must be at least 10 characters.");
        }
        if (!newPassword.equals(confirmNewPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        String encodedPasswordFromDb = user.getPassword();
        if (encodedPasswordFromDb == null || encodedPasswordFromDb.isBlank()) {
            throw new IllegalArgumentException("User password is not set.");
        }

        if (!passwordEncoder.matches(oldPassword, encodedPasswordFromDb)) {
            throw new IllegalArgumentException("Old password is incorrect.");
        }

        // Optional: prevent reusing same password
        if (passwordEncoder.matches(newPassword, encodedPasswordFromDb)) {
            throw new IllegalArgumentException("New password must be different.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /*********** Update Addresses ***************/

    public void updateAddresses(String userId, AddressUpdateWrapper form) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User not found.");
        }
        if (form == null) {
            throw new IllegalArgumentException("Address form payload is missing.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        List<User.Address> merged = new ArrayList<>();
        if (form.getAddresses() != null) merged.addAll(form.getAddresses());
        if (form.getNewAddresses() != null) merged.addAll(form.getNewAddresses());

        // If the user submitted nothing useful
        if (merged.isEmpty()) {
            throw new IllegalArgumentException("No addresses were submitted.");
        }

        // Remove empty rows (common when user adds a block then leaves it blank)
        merged.removeIf(this::isBlankAddress);

        if (merged.isEmpty()) {
            throw new IllegalArgumentException("No valid addresses were submitted.");
        }

        // Normalize fields (trim strings)
        for (User.Address a : merged) {
            if (a.getType() != null) a.setType(a.getType().trim().toUpperCase());
            if (a.getStreet() != null) a.setStreet(a.getStreet().trim());
            if (a.getCity() != null) a.setCity(a.getCity().trim());
            if (a.getState() != null) a.setState(a.getState().trim());
            if (a.getZip() != null) a.setZip(a.getZip().trim());

            if (isBlank(a.getType())) {
                throw new IllegalArgumentException("Address type is required (SHIPPING, BILLING, or ALTERNATE).");
            }
            if (!ALLOWED_ADDRESS_TYPES.contains(a.getType())) {
                throw new IllegalArgumentException(
                        "Invalid address type: " + a.getType() + ". Allowed: SHIPPING, BILLING, ALTERNATE."
                );
            }

            if (isBlank(a.getStreet())) throw new IllegalArgumentException("Street is required for each address.");
            if (isBlank(a.getCity())) throw new IllegalArgumentException("City is required for each address.");
            if (isBlank(a.getState())) throw new IllegalArgumentException("State is required for each address.");
            if (isBlank(a.getZip())) throw new IllegalArgumentException("Zip is required for each address.");

            AddressDTO input = new AddressDTO(a.getStreet(), a.getCity(), a.getState(), a.getZip());
            ValidatedAddress validated = googleAddressValidationService.validateUsHighCertainty(input);
            serviceAreaValidationService.enforceWithinRadiusOrThrow(validated);
        }

        // Enforce single default (if multiple checked, keep the last checked as default)
        int lastDefaultIndex = -1;
        for (int i = 0; i < merged.size(); i++) {
            if (merged.get(i).isDefault()) lastDefaultIndex = i;
        }
        if (lastDefaultIndex >= 0) {
            for (int i = 0; i < merged.size(); i++) {
                merged.get(i).setDefault(i == lastDefaultIndex);
            }
        }

        user.setAddresses(merged);
        userRepository.save(user);
    }

    private boolean isBlankAddress(User.Address a) {
        return a == null || (
                isBlank(a.getType()) &&
                        isBlank(a.getStreet()) &&
                        isBlank(a.getCity()) &&
                        isBlank(a.getState()) &&
                        isBlank(a.getZip())
        );
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

}
