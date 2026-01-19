package com.ua.estore.cgsWeb.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**") // Protect everything...
                .excludePathPatterns("/login", "/logout", "/css/**", "/images/**", "/js/**", "/about"); // ...except these
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");

        String absolutePath = new File(uploadPath).getAbsolutePath();
        if (!absolutePath.endsWith(File.separator)) {
            absolutePath += File.separator;
        }

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .addResourceLocations("file:" + absolutePath + "/")
                .addResourceLocations("file:" + absolutePath + "site-images/")
                .addResourceLocations("file:" + absolutePath + "vendors/")
                .addResourceLocations("file:" + absolutePath + "products/");
    }
}