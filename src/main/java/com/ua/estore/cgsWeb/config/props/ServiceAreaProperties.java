package com.ua.estore.cgsWeb.config.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "service.area")
public record ServiceAreaProperties(double originLat, double originLng, double radiusMiles) {}
