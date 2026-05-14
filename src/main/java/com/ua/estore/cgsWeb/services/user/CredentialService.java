package com.ua.estore.cgsWeb.services.user;

import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
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

    public boolean checkUsername(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    /**************************************************************************
     * Authentication
     *************************************************************************/

    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return userOpt;
        }
        return Optional.empty();
    }

    /**************************************************************************
     * Save New User
     *************************************************************************/

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

    /**************************************************************************
     * Update User Password
     *************************************************************************/

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

    /**************************************************************************
     * Reset User Password — token-authorized, NO current password required.
     *
     * This is the password-reset-flow counterpart to updatePassword(). The
     * difference: updatePassword() proves identity with the OLD password;
     * resetPassword() relies on the caller having already consumed a valid
     * PASSWORD_RESET token (TokenService.consume), which is itself proof the
     * request reached the inbox owner. So there is deliberately no
     * old-password check here — the user is resetting precisely because they
     * can't supply it.
     *
     * Everything else mirrors updatePassword(): same length rule, same
     * match rule, same must-be-different rule, same encode-and-save.
     *
     * IMPORTANT: this method assumes the token has ALREADY been validated and
     * consumed by the caller. It does not touch tokens itself — keeping token
     * mechanics in TokenService and password mechanics here.
     *************************************************************************/

    public void resetPassword(String userId, String newPassword, String confirmNewPassword) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User not found.");
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

        // Prevent reusing the same password when there's an existing one to
        // compare against. (A user with no password set — edge case — just
        // gets the new one.)
        String encodedPasswordFromDb = user.getPassword();
        if (encodedPasswordFromDb != null && !encodedPasswordFromDb.isBlank()
                && passwordEncoder.matches(newPassword, encodedPasswordFromDb)) {
            throw new IllegalArgumentException("New password must be different from your current one.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset completed for userId={}", userId);
    }

    /**************************************************************************
     * Mark Email Verified — flips a freshly-confirmed account live.
     *
     * Called by the verification flow after TokenService.consume() succeeds
     * on an EMAIL_VERIFICATION token. Sets both flags:
     *   - emailVerified = true  (they proved inbox ownership)
     *   - enabled       = true  (the account may now log in)
     *
     * Idempotent: calling it on an already-verified user is a harmless no-op
     * re-save. That matters because a user could click the verification link
     * twice — the token's single-use guard stops the SECOND consume, but if
     * something retries at this layer, re-running it does no damage.
     *************************************************************************/

    public void markEmailVerified(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User not found.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (user.isEmailVerified() && user.isEnabled()) {
            log.debug("markEmailVerified: userId={} already verified+enabled, no-op", userId);
            return;
        }

        user.setEmailVerified(true);
        user.setEnabled(true);
        userRepository.save(user);
        log.info("Email verified and account enabled for userId={}", userId);
    }
}
