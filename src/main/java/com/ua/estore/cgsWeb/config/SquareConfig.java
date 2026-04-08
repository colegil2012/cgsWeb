package com.ua.estore.cgsWeb.config;

import com.squareup.square.SquareClient;
import com.squareup.square.core.Environment;
import com.ua.estore.cgsWeb.config.props.SquareProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SquareConfig {

    @Bean
    public SquareClient squareClient(SquareProperties props) {

        Environment env = Environment.SANDBOX;
        String accessToken = props.sandbox() != null ? props.sandbox().accessToken() : null;

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Square access token is required");
        }

        return SquareClient.builder()
                .environment(env)
                .token(accessToken)
                .build();
    }
}
