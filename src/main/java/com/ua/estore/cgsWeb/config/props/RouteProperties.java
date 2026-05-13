package com.ua.estore.cgsWeb.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for route generation. Bound to {@code celtech.route.*} in
 * {@code application.yml}.
 *
 * <p>The {@link #optimizer} field selects which {@code RouteOptimizer}
 * implementation Spring wires up via {@code @ConditionalOnProperty}. Currently
 * the only implementation is ORS; future implementations will register under
 * different name values ("google", "or-tools", etc.).</p>
 */
@ConfigurationProperties(prefix = "celtech.route")
public record RouteProperties(
        String optimizer,
        HomeBase homeBase,
        Ors ors
) {
    /** Shop's starting point. Used as origin for any route that doesn't override. */
    public record HomeBase(double latitude, double longitude) {}

    /** ORS-specific configuration. Ignored if {@link RouteProperties#optimizer} != "ors". */
    public record Ors(
            String apiKey,
            String baseUrl,
            String profile,
            int timeoutSeconds
    ) {}
}