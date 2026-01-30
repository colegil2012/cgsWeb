package com.ua.estore.cgsWeb.services;

import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
@RequiredArgsConstructor
public class CredentialService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

}
