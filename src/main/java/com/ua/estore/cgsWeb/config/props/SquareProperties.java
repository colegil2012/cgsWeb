package com.ua.estore.cgsWeb.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "square")
public record SquareProperties(
        String environment,
        Sandbox sandbox,
        String applicationId,
        String locationId) {
    public record Sandbox(String accessToken) {}
}
