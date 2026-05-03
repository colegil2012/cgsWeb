package com.ua.estore.cgsWeb.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties (
        boolean enabled,
        String from,
        String fromName,
        String replyTo,
        String publicBaseUrl
    ) {}
