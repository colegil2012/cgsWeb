package com.ua.estore.cgsWeb.config;

import com.ua.estore.cgsWeb.config.props.SecurityProperties;
import com.ua.estore.cgsWeb.security.AuthSuccessHandler;
import com.ua.estore.cgsWeb.security.CustomUserDetailsService;
import com.ua.estore.cgsWeb.security.GuestCookieCleanupFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityProperties securityProperties;

    /* -------- Password encoders -------- */

    /**
     * Delegating encoder: can READ any hash prefix ({bcrypt}, {noop}, legacy $2a$...),
     * but always WRITES new hashes at our configured bcrypt strength.
     * This means existing seeded hashes authenticate fine, and when a user next
     * logs in (or changes their password) we transparently upgrade to the new strength.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        int strength = securityProperties.bcrypt().strength();
        BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder(strength);

        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", bcrypt);

        DelegatingPasswordEncoder delegating = new DelegatingPasswordEncoder("bcrypt", encoders);
        // Treat bare $2a$/$2b$/$2y$ hashes (no {id} prefix) as bcrypt — this is what our seed data has.
        delegating.setDefaultPasswordEncoderForMatches(bcrypt);
        return delegating;
    }

    /* -------- Authentication manager -------- */

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService uds,
                                                            PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    /* -------- Remember-me -------- */

    @Bean
    public PersistentTokenRepository persistentTokenRepository(MongoTemplate mongoTemplate) {
        return new MongoPersistentTokenRepository(mongoTemplate);
    }

    @Bean
    public PersistentTokenBasedRememberMeServices rememberMeServices(PersistentTokenRepository repo,
                                                                     CustomUserDetailsService uds) {
        PersistentTokenBasedRememberMeServices services =
                new PersistentTokenBasedRememberMeServices(
                        securityProperties.rememberMe().key(), uds, repo);
        services.setTokenValiditySeconds(securityProperties.rememberMe().validitySeconds());
        services.setParameter("remember-me");
        services.setCookieName("cgs_remember_me");
        services.setUseSecureCookie(false); // overridden to true in production via a profile if desired
        return services;
    }

    /* -------- Filter chain -------- */

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        // Protects against session fixation: mints a new session id after login.
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthSuccessHandler successHandler,
                                           PersistentTokenBasedRememberMeServices rememberMeServices,
                                           GuestCookieCleanupFilter guestCookieCleanupFilter) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        // Static resources — listed first so they bypass everything else cleanly
                        .requestMatchers(
                                "/css/**",
                                "/images/**",
                                "/scripts/**",
                                "/js/**",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()
                        // Fully public pages
                        .requestMatchers(
                                "/", "/about",
                                "/login", "/logout",
                                "/signup/**"
                        ).permitAll()
                        // Guest-friendly: browsing the catalog and using a cart does NOT require auth
                        .requestMatchers(
                                "/shop/**",
                                "/vendors", "/vendor/{id:[0-9a-fA-F]{24}}",
                                "/cart", "/cart/**",
                                "/api/shipping/estimate",
                                "/api/address/**"
                        ).permitAll()
                        // Role-gated
                        .requestMatchers("/vendor/portal/**", "/vendor/addresses").hasRole("VENDOR")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Everything else (account, checkout) requires auth
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")     // POST target — Spring intercepts this
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "cgs_remember_me")
                        .permitAll()
                )
                .rememberMe(rm -> rm
                        .rememberMeServices(rememberMeServices)
                        .key(securityProperties.rememberMe().key())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::changeSessionId)
                        .maximumSessions(5)   // allow multiple devices, but cap it
                )
                .csrf(csrf -> csrf
                        // Session-backed token. The CsrfTokenModelInterceptor pushes the
                        // token into the MVC model so Groovy templates can render it.
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                )
                .addFilterAfter(guestCookieCleanupFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}