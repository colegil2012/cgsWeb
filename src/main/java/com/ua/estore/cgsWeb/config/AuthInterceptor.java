package com.ua.estore.cgsWeb.config;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             @Nonnull HttpServletResponse response,
                             @Nonnull Object handler) throws Exception {
        HttpSession session = request.getSession();

        if(session.getAttribute("user") == null) {
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}
