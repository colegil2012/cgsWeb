package com.ua.estore.cgsWeb.security;

import com.ua.estore.cgsWeb.services.user.GuestIdentityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Watches for the "cgs.clearGuestCookie" flag set by {@link AuthSuccessHandler}
 * and, if present, clears the guest cookie on the outgoing response.
 * This is a tiny dedicated filter rather than doing it inline because the handler
 * delegates to super.onAuthenticationSuccess which commits the response.
 */
@Component
@RequiredArgsConstructor
public class GuestCookieCleanupFilter extends OncePerRequestFilter {

    private final GuestIdentityService guestIdentityService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);

        Object flag = request.getAttribute("cgs.clearGuestCookie");
        if (Boolean.TRUE.equals(flag) && !response.isCommitted()) {
            guestIdentityService.clearGuestCookie(request, response);
        }
    }
}