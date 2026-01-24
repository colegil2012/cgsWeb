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
}
