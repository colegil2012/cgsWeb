package com.ua.estore.cgsWeb.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Pushes the Spring Security CSRF token into the ModelAndView so that
 * Groovy Markup Templates (which read from the Model, not from request attributes)
 * can access it via ${csrfToken}, ${csrfParamName}, ${csrfHeaderName}.
 *
 * This is the documented fix for the well-known issue where Groovy / FreeMarker
 * templates cannot see the _csrf request attribute that Spring Security exposes.
 * A HandlerInterceptor.postHandle() is the correct lifecycle hook — it runs after
 * the controller returns but before the view is rendered, which is exactly when
 * the Model is assembled for the template engine.
 *
 * We also eagerly call token.getToken() here. Spring 6 uses a "deferred" CsrfToken
 * by default to avoid loading the session on every request; calling getToken()
 * forces resolution of the masked token value so the template sees a real string.
 */
@Component
public class CsrfTokenModelInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull Object handler,
                           @Nullable ModelAndView modelAndView) {
        // No-op for REST controllers (no ModelAndView) and for redirects
        if (modelAndView == null || modelAndView.getViewName() == null
                || modelAndView.getViewName().startsWith("redirect:")) {
            return;
        }

        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token == null) {
            token = (CsrfToken) request.getAttribute("_csrf");
        }

        if (token != null) {
            // Force token resolution (deferred → realized)
            String tokenValue = token.getToken();
            modelAndView.addObject("csrfToken", tokenValue);
            modelAndView.addObject("csrfParamName", token.getParameterName());
            modelAndView.addObject("csrfHeaderName", token.getHeaderName());
            // Keep legacy `_csrf` as a convenience so existing templates that already
            // reference _csrf.token keep working. This IS the actual CsrfToken object.
            modelAndView.addObject("_csrf", token);
        } else {
            // Safe fallbacks so templates never render exception
            modelAndView.addObject("csrfToken", "");
            modelAndView.addObject("csrfParamName", "_csrf");
            modelAndView.addObject("csrfHeaderName", "X-CSRF-TOKEN");
        }
    }
}