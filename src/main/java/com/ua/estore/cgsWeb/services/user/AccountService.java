package com.ua.estore.cgsWeb.services.user;

import com.ua.estore.cgsWeb.config.props.MailProperties;
import com.ua.estore.cgsWeb.models.token.TokenType;
import com.ua.estore.cgsWeb.models.token.VerificationToken;
import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.repositories.user.UserRepository;
import com.ua.estore.cgsWeb.services.mail.PasswordResetMailer;
import com.ua.estore.cgsWeb.services.mail.SignupConfirmationMailer;
import com.ua.estore.cgsWeb.services.token.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestrates the email-verification and password-reset flows — the glue
 * between {@code TokenService} (token mechanics), the mailers (message
 * delivery), and {@code CredentialService} (user mutations).
 *
 * <p>The controller layer talks to this service; this service coordinates
 * the others. Keeps the controllers thin and keeps token/password mechanics
 * in their own dedicated services.</p>
 *
 * <h3>Why the async boundary lives here</h3>
 * {@link #sendPasswordResetEmail} is {@code @Async}. The password-reset
 * <em>request</em> endpoint must not leak — via response timing — whether an
 * email belongs to a real account. If "email exists" did real work
 * (DB lookup, token issue, two template renders, mail handoff) on the
 * request thread while "email doesn't exist" returned instantly, an attacker
 * timing responses could enumerate accounts.
 *
 * <p>The fix: the controller calls {@link #sendPasswordResetEmail} and
 * <em>immediately</em> returns the generic response. ALL conditional work —
 * the lookup, and the issue/render/send if a user is found — happens on a
 * background thread. The HTTP response timing is identical either way
 * because the controller does nothing conditional before returning.</p>
 *
 * <p>Note: {@code @Async} requires {@code @EnableAsync} somewhere in the
 * application config. {@code JavaMailService.send()} is already
 * {@code @Async}, so this must already be present — if it weren't, mail
 * would currently be sending synchronously.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final CredentialService credentialService;
    private final SignupConfirmationMailer signupConfirmationMailer;
    private final PasswordResetMailer passwordResetMailer;
    private final MailProperties mailProps;

    /* ========================================================================
     * Email verification
     * ===================================================================== */

    /**
     * Issue an email-verification token for a freshly-registered user and
     * send the confirmation email. Called synchronously from the signup flow
     * — there's no enumeration concern at signup (the user just told us their
     * email by typing it into the form), so this doesn't need the async
     * treatment that password-reset does.
     *
     * <p>Failures here are logged but not rethrown — a signup shouldn't hard-
     * fail just because the mail subsystem hiccuped. The user can request a
     * fresh verification email from the login page.</p>
     *
     * @param user the just-saved user (must have an id and a non-blank email)
     */
    public void sendVerificationEmail(User user) {
        if (user == null || user.getId() == null
                || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("sendVerificationEmail: user/id/email missing, skipping");
            return;
        }
        if (mailProps.enabled()) {
            try {
                String rawToken = tokenService.issue(user.getId(), TokenType.EMAIL_VERIFICATION);
                signupConfirmationMailer.sendFor(user, rawToken);
            } catch (RuntimeException ex) {
                log.error("Failed to issue/send verification email for userId={}", user.getId(), ex);
            }
        } else {
            log.info("Mail Service is disabled. Verification emails will not be sent.");
        }
    }

    /**
     * Re-send a verification email, given an email address. Fire-and-forget
     * and {@code @Async} for the same enumeration-oracle reason as
     * {@link #sendPasswordResetEmail} — the resend endpoint must not leak,
     * via response timing or content, whether the address maps to a real
     * unverified account.
     *
     * <p>Does the lookup itself, on the background thread. Only acts if the
     * email maps to a user who exists AND is still unverified — re-sending a
     * verification link to an already-verified account would be pointless and
     * mildly confusing.</p>
     *
     * @param email the email address from the resend form
     */
    @Async
    public void resendVerificationEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        String normalized = email.trim();

        User user = userRepository.findByEmail(normalized).orElse(null);
        if (user == null) {
            log.debug("Verification resend requested for unknown email; no action taken");
            return;
        }
        if (user.isEmailVerified()) {
            // Already verified — nothing to do. Don't issue a token or send
            // mail; the account is fine.
            log.debug("Verification resend requested for already-verified userId={}; skipping",
                    user.getId());
            return;
        }

        if (mailProps.enabled()) {
            try {
                String rawToken = tokenService.issue(user.getId(), TokenType.EMAIL_VERIFICATION);
                signupConfirmationMailer.sendFor(user, rawToken);
                log.info("Verification email re-sent for userId={}", user.getId());
            } catch (RuntimeException ex) {
                log.error("Failed to re-send verification email for userId={}", user.getId(), ex);
            }
        } else {
            log.info("Mail Service is disabled. Verification emails will not be sent.");
        }
    }

    /**
     * Validate a verification token and, if good, flip the account live.
     *
     * @param rawToken the token from the email link
     * @return true if the token was valid and the account is now verified;
     *         false for any failure (unknown/expired/used/wrong-type token).
     *         The caller cannot tell which — deliberate, see TokenService.
     */
    public boolean confirmEmailVerification(String rawToken) {
        Optional<VerificationToken> consumed =
                tokenService.consume(rawToken, TokenType.EMAIL_VERIFICATION);

        if (consumed.isEmpty()) {
            return false;
        }

        try {
            credentialService.markEmailVerified(consumed.get().getUserId());
            return true;
        } catch (IllegalArgumentException ex) {
            // Token pointed at a user that no longer exists, etc. The token's
            // already been consumed at this point — that's fine, it's a dead
            // token for a dead user.
            log.warn("Verification token consumed but user mutation failed: {}", ex.getMessage());
            return false;
        }
    }

    /* ========================================================================
     * Password reset
     * ===================================================================== */

    /**
     * Fire-and-forget password-reset email send. See the class Javadoc for
     * why this is {@code @Async} — it's the timing-oracle defense.
     *
     * <p>This method does the email lookup ITSELF (rather than taking a
     * {@code User}) so that the "user not found" branch also runs on the
     * background thread. The controller hands in a raw email string and
     * never learns the outcome.</p>
     *
     * <p>All failure modes are silent (logged only): unknown email is a
     * normal, expected case, not an error; mail hiccups shouldn't surface
     * anywhere the requester can observe.</p>
     *
     * @param email the email address from the request form
     */
    @Async
    public void sendPasswordResetEmail(String email) {
        if (email == null || email.isBlank()) {
            // Shouldn't reach here — controller validates presence — but the
            // async method is defensive since it has no caller to report to.
            return;
        }
        String normalized = email.trim();

        User user = userRepository.findByEmail(normalized).orElse(null);
        if (user == null) {
            // Expected, normal case. Log at debug so it's available when
            // chasing "I didn't get my reset email" support questions, but
            // it isn't noise at info level.
            log.debug("Password-reset requested for unknown email; no action taken");
            return;
        }

        if (mailProps.enabled()) {
            try {
                String rawToken = tokenService.issue(user.getId(), TokenType.PASSWORD_RESET);
                passwordResetMailer.sendFor(user, rawToken);
                log.info("Password-reset email dispatched for userId={}", user.getId());
            } catch(RuntimeException ex){
                log.error("Failed to issue/send password-reset email for userId={}", user.getId(), ex);
            }
        } else {
            log.info("Mail Service is disabled. Password reset emails will not be sent.");
        }
    }

    /**
     * Validate a password-reset token without consuming it — used to decide
     * whether to render the "choose a new password" form or an
     * "invalid/expired link" page. The token is only burned on the actual
     * POST (see {@link #performPasswordReset}).
     *
     * @param rawToken the token from the email link
     * @return true if the token is currently valid and unused
     */
    public boolean isResetTokenValid(String rawToken) {
        return tokenService.peek(rawToken, TokenType.PASSWORD_RESET).isPresent();
    }

    /**
     * Consume a password-reset token and apply the new password.
     *
     * <p>Order matters: the token is consumed FIRST. If
     * {@code consume} succeeds but the password update then fails validation
     * (too short, mismatch), the token is already spent — the user has to
     * request a fresh reset link. That's the safe direction to fail: a
     * half-applied reset can't leave a token replayable. The form should do
     * client-side validation so this is rare, and the server-side messages
     * still tell the user exactly what was wrong.</p>
     *
     * @param rawToken           token from the reset form (hidden field)
     * @param newPassword        the chosen password
     * @param confirmNewPassword confirmation copy
     * @return true on success; false if the token was invalid/expired/used
     * @throws IllegalArgumentException if the token was valid but the
     *         password failed validation (message is user-safe)
     */
    public boolean performPasswordReset(String rawToken,
                                        String newPassword,
                                        String confirmNewPassword) {
        Optional<VerificationToken> consumed =
                tokenService.consume(rawToken, TokenType.PASSWORD_RESET);

        if (consumed.isEmpty()) {
            return false;
        }

        // Token is now spent. Apply the password — may throw
        // IllegalArgumentException for validation failures, which the
        // controller surfaces to the form.
        credentialService.resetPassword(
                consumed.get().getUserId(), newPassword, confirmNewPassword);
        return true;
    }
}