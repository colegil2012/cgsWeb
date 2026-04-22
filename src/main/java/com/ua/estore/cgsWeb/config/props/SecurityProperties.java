package com.ua.estore.cgsWeb.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        Bcrypt bcrypt,
        RememberMe rememberMe,
        GuestCart guestCart
) {
    public record Bcrypt(int strength) {}
    public record RememberMe(String key, int validitySeconds) {}
    public record GuestCart(String cookieName, int maxAgeDays) {}
}
