package com.ua.estore.cgsWeb.config;

import com.ua.estore.cgsWeb.config.props.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        GoogleMapsProperties.class,
        ServiceAreaProperties.class,
        SpacesS3Properties.class,
        SecurityProperties.class,
        TaxProperties.class,
        MailProperties.class,
        RouteProperties.class
})

public class AppPropertiesConfig {}
