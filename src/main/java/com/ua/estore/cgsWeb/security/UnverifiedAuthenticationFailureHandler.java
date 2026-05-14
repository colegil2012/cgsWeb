package com.ua.estore.cgsWeb.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Login failure handler that tells an unverified user something useful.
 *
 * <p>Spring's default {@code failureUrl("/login?error")} is a single static
 * URL — it can't vary by failure reason. But "wrong password" and "correct
 * password, account not verified" are very different situations for the
 * user, and showing "invalid username or password" to someone whose
 * credentials are actually <em>correct</em> (they just haven't clicked the
 * verification link) is confusing and generates support questions.</p>
 *
 * <p>When {@code CustomUserDetails.isEnabled()} returns false — which it does
 * for unverified accounts — {@code DaoAuthenticationProvider} throws
 * {@link DisabledException} <em>before</em> checking the password. This
 * handler catches that specific case and routes to
 * {@code /login?error=unverified}; everything else falls through to the
 * normal {@code /login?error}. {@code UserController.login()} reads the
 * param and picks the message.</p>
 *
 * <p>Subtle point worth keeping in mind: because {@code DisabledException}
 * is thrown before the password check, a login attempt against an
 * unverified account produces {@code error=unverified} <em>regardless of
 * whether the password was right</em>. That's a minor information disclosure
 * — it confirms the account exists and is unverified. For a storefront this
 * is an accepted trade for the much better UX; if it ever matters, the
 * alternative is to keep the generic message and accept the support load.</p>
 */
@Component
public class UnverifiedAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException {

        if (exception instanceof DisabledException) {
            getRedirectStrategy().sendRedirect(request, response, "/login?error=unverified");
            return;
        }

        // Default behavior for every other failure (bad credentials, locked,
        // expired, etc.) — mirrors the old static failureUrl("/login?error").
        getRedirectStrategy().sendRedirect(request, response, "/login?error");
    }
}