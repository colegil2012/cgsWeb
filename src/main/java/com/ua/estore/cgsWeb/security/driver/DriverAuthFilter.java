package com.ua.estore.cgsWeb.security.driver;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Static-bearer-token gate for the {@code /api/driver/**} endpoints.
 *
 * <p>Reads the expected token from its constructor, not from {@code @Value} or a
 * {@code @Component} annotation. This is deliberate: Spring Boot auto-registers
 * any {@code Filter} bean it finds in the application context as a global servlet
 * filter applied to <em>every</em> request — which would cause this filter to
 * reject storefront traffic too. Instantiating it directly inside
 * {@link com.ua.estore.cgsWeb.config.DriverSecurityConfig#driverFilterChain}
 * avoids that auto-registration entirely.</p>
 *
 * <p>On success, this filter populates {@link SecurityContextHolder} with a
 * {@code ROLE_DRIVER} authority so downstream authorization rules can refer
 * to it.</p>
 *
 * <p><b>What this is NOT:</b></p>
 * <ul>
 *   <li>Per-driver auth — every device using the same token is indistinguishable.
 *       Fine for owner-operator phase. Replace with proper per-device tokens or
 *       OAuth before multi-driver rollout.</li>
 *   <li>Sufficient on its own over the open internet — pair it with HTTPS so the
 *       token isn't transmitted in plaintext. Fine on a LAN for development.</li>
 * </ul>
 */
@Slf4j
public class DriverAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final String expectedToken;

    public DriverAuthFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Let CORS preflights through unauthenticated. Browsers send OPTIONS
        // without the Authorization header on cross-origin requests with
        // custom headers; if we reject them here, the actual request never fires.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Fail closed if the property wasn't set — silently allowing requests
        // through would be the worst possible default.
        if (expectedToken == null || expectedToken.isBlank()) {
            log.error("celtech.driver.token is not configured — rejecting driver request to {}",
                    request.getRequestURI());
            writeUnauthorized(response, "Driver auth not configured");
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            log.warn("Driver request to {} missing bearer token", request.getRequestURI());
            writeUnauthorized(response, "Missing bearer token");
            return;
        }

        String presented = header.substring(BEARER_PREFIX.length()).trim();
        if (!constantTimeEquals(expectedToken, presented)) {
            log.warn("Driver request to {} presented an invalid token", request.getRequestURI());
            writeUnauthorized(response, "Invalid token");
            return;
        }

        // Populate the security context so the request is properly "authenticated"
        // for the rest of the filter chain. Anonymous principal name "driver-kiosk"
        // — replace with a real device identifier once per-device tokens land.
        DriverAuthentication auth = new DriverAuthentication(
                "driver-kiosk",
                List.of(new SimpleGrantedAuthority("ROLE_DRIVER"))
        );
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            chain.doFilter(request, response);
        } finally {
            // Clear the context so the thread doesn't carry it into the next
            // request (matters in container-managed thread pools).
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Constant-time string comparison so an attacker can't time the byte-by-byte
     * mismatch to recover the token.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    private static void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    /**
     * Minimal Authentication implementation. We don't need a UserDetails since
     * the token isn't tied to a database user — it's a device shared secret.
     */
    private static final class DriverAuthentication extends AbstractAuthenticationToken {
        private final String principal;

        DriverAuthentication(String principal, List<SimpleGrantedAuthority> authorities) {
            super(authorities);
            this.principal = principal;
        }

        @Override public Object getCredentials() { return ""; }
        @Override public Object getPrincipal()   { return principal; }
    }
}