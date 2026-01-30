package com.ua.estore.cgsWeb.config;

import com.ua.estore.cgsWeb.config.props.SpacesS3Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@Profile("prod")
public class SpacesS3Config {

    @Bean
    public S3Client s3Client(SpacesS3Properties props) {
        return S3Client.builder()
                .endpointOverride(URI.create(props.endpoint()))
                .region(Region.of(props.region()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(props.accessKey(), props.secretKey())
                        )
                )
                .build();
    }
}
