package com.ua.estore.cgsWeb.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class requestUtil {

    public String buildFullUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String qs = request.getQueryString();
        return (qs == null || qs.isBlank()) ? uri : uri + "?" + qs;
    }

    public static String getReferalUrl(String referer, String fallback) {
        if (referer == null || referer.isBlank()) return fallback;
        if (referer.contains("/cart")) return "/cart";
        if (referer.contains("/account")) return "/account?tab=addresses";
        if (referer.contains("/vendor/portal")) return "/vendor/portal";

        return fallback;
    }
}
