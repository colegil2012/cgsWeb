package com.ua.estore.cgsWeb.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.spaces")
public record SpacesS3Properties(
        String bucket,
        String region,
        String endpoint,
        String accessKey,
        String secretKey
) {}
