package com.ua.estore.cgsWeb.config;

import com.ua.estore.cgsWeb.config.props.GoogleMapsProperties;
import com.ua.estore.cgsWeb.config.props.ServiceAreaProperties;
import com.ua.estore.cgsWeb.config.props.SpacesS3Properties;
import com.ua.estore.cgsWeb.config.props.SquareProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GoogleMapsProperties.class, ServiceAreaProperties.class, SpacesS3Properties.class, SquareProperties.class})
public class AppPropertiesConfig {}
