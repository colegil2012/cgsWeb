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

    public boolean isValidUser(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return user.getPassword().equals(password);
        }

        return false;
    }
}
