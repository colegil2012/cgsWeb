package com.ua.estore.cgsWeb.services.user;

import com.ua.estore.cgsWeb.config.props.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages the `cgs_guest_id` cookie that identifies a visitor's cart when they're
 * not signed in. Creates one lazily on first use; reads it on every subsequent request.
 */
@Service
@RequiredArgsConstructor
public class GuestIdentityService {

    private final SecurityProperties securityProperties;

    /**
     * Returns the existing guest id from the cookie, or null if none is present.
     * Does NOT create one — use {@link #ensureGuestId} if you want to mint-on-read.
     */
    public String readGuestId(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) return null;
        String name = cookieName();
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }

    /**
     * Returns the existing guest id, or creates a new one and writes the cookie.
     */
    public String ensureGuestId(HttpServletRequest request, HttpServletResponse response) {
        String existing = readGuestId(request);
        if (existing != null) return existing;

        String fresh = UUID.randomUUID().toString();
        writeCookie(request, response, fresh);
        return fresh;
    }

    /**
     * Removes the guest cookie (e.g. after merging a guest cart into a user's cart on login).
     */
    public void clearGuestCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName(), "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setSecure(request != null && request.isSecure());
        response.addCookie(cookie);
    }

    private void writeCookie(HttpServletRequest request, HttpServletResponse response, String value) {
        int maxAgeSeconds = (int) TimeUnit.DAYS.toSeconds(securityProperties.guestCart().maxAgeDays());
        // Spring Boot's Cookie doesn't support SameSite directly; add it via Set-Cookie header.
        StringBuilder header = new StringBuilder()
                .append(cookieName()).append('=').append(value)
                .append("; Path=/")
                .append("; Max-Age=").append(maxAgeSeconds)
                .append("; HttpOnly")
                .append("; SameSite=Lax");
        if (request != null && request.isSecure()) {
            header.append("; Secure");
        }
        response.addHeader("Set-Cookie", header.toString());
    }

    private String cookieName() {
        return securityProperties.guestCart().cookieName();
    }
}