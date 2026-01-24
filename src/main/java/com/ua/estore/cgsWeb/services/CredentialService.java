package com.ua.estore.cgsWeb.services;

import com.ua.estore.cgsWeb.models.User;
import com.ua.estore.cgsWeb.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CredentialService {

    private final UserRepository userRepository;

    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            return userOpt;
        }
        return Optional.empty();
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
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

        userRepository.save(user);
        return "User saved successfully";
    }

}
