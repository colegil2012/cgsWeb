package com.ua.estore.cgsWeb.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Token lifetime configuration, bound to {@code app.tokens.*} in
 * {@code application.yml}.
 *
 * <p>Verification and reset tokens get deliberately different windows.
 * A signup verification link can be generous — people sign up and check
 * email later. A password-reset link should be short — it grants account
 * takeover, not just activation, so a tighter window limits exposure if
 * the email is intercepted or left open on a shared machine.</p>
 *
 * <p>{@link Duration} fields accept {@code application.yml} values like
 * {@code 48h} and {@code 30m}.</p>
 */
@ConfigurationProperties(prefix = "app.tokens")
public record TokenProperties(
        Duration verificationTtl,
        Duration passwordResetTtl
) {
    /** Defensive defaults if the properties are absent. */
    public TokenProperties {
        if (verificationTtl == null)  verificationTtl  = Duration.ofHours(48);
        if (passwordResetTtl == null) passwordResetTtl = Duration.ofMinutes(30);
    }
}