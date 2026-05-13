package com.ua.estore.cgsWeb.config.driver;

import com.ua.estore.cgsWeb.security.driver.DriverAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Dedicated {@link SecurityFilterChain} for the driver-app HTTP surface.
 *
 * <p>Spring Security supports multiple filter chains — the first one whose
 * {@code securityMatcher} accepts the request handles it. We give this chain
 * {@link Ordered#HIGHEST_PRECEDENCE} so {@code /api/driver/**} requests are
 * matched here before the storefront's chain has a chance to redirect them
 * to {@code /login}.</p>
 *
 * <p><b>CORS:</b> handled at the chain level via {@link #driverCorsConfigurationSource()},
 * not via {@code @CrossOrigin} on the controller. Spring Security's CORS filter
 * runs <em>before</em> authentication, so browser preflight {@code OPTIONS}
 * requests get a proper {@code Access-Control-Allow-Origin} response without
 * ever hitting the bearer-token check (which they'd fail, since browsers don't
 * send custom headers on preflights).</p>
 *
 * <p><b>Filter registration:</b> {@link DriverAuthFilter} is instantiated directly
 * inside the chain builder, not as a Spring {@code @Bean}. Spring Boot
 * auto-registers any {@code Filter} bean as a global servlet filter applied to
 * every request — direct instantiation here keeps it scoped to this chain only.</p>
 */
@Configuration
public class DriverSecurityConfig {

    @Value("${celtech.driver.token:}")
    private String driverToken;

    /**
     * CORS rules for {@code /api/driver/**}.
     *
     * <p>Currently permissive — any origin can call these endpoints as long as it
     * presents a valid bearer token. The kiosk runs from {@code file://} (origin
     * "null") on the Pi, and from IntelliJ's dev server (different port, different
     * origin) during local development. Tighten this once the deployment story is
     * settled — at minimum, replace {@code allowedOriginPatterns("*")} with the
     * specific origins you actually use.</p>
     */
    @Bean
    public CorsConfigurationSource driverCorsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Use allowedOriginPatterns (not allowedOrigins) so we can combine wildcard
        // matching with allowCredentials. allowedOrigins("*") would conflict with
        // allowCredentials(true), but the storefront uses cookies so leaving that
        // door closed for the driver chain is fine — credentials aren't needed,
        // bearer tokens travel in headers.
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L); // cache preflight responses for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/driver/**", config);
        return source;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain driverFilterChain(HttpSecurity http,
                                                 CorsConfigurationSource driverCorsConfigurationSource) throws Exception {

        // Construct the filter here. Not a Spring bean, so Spring Boot can't
        // auto-register it as a global servlet filter.
        DriverAuthFilter driverAuthFilter = new DriverAuthFilter(driverToken);

        http
                // Scope: only requests to /api/driver/** are handled by this chain.
                .securityMatcher("/api/driver/**")

                // CORS first — preflight OPTIONS requests get headers added by
                // Spring Security's CORS filter, which sits before authentication.
                .cors(cors -> cors.configurationSource(driverCorsConfigurationSource))

                .authorizeHttpRequests(auth -> auth
                        // Permit preflight OPTIONS without auth. Belt-and-suspenders
                        // with the CORS filter above — if anything ever reorders the
                        // filter chain, the auth rule still lets OPTIONS through.
                        .requestMatchers(HttpMethod.OPTIONS, "/api/driver/**").permitAll()
                        .anyRequest().hasRole("DRIVER")
                )

                // Stateless: no JSESSIONID, no sessions for these endpoints.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // CSRF tokens are session-bound; they don't apply to a stateless
                // bearer-token API.
                .csrf(csrf -> csrf.disable())

                // No form login, no HTTP basic, no logout — the kiosk doesn't
                // need any of these.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())

                // Wire our bearer-token filter where UsernamePasswordAuthenticationFilter
                // would normally sit.
                .addFilterBefore(driverAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}