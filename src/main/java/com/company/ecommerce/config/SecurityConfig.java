package com.company.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline security configuration for the foundation phase.
 *
 * <p>The API is stateless. Actuator and OpenAPI endpoints are public so health checks and Swagger
 * remain reachable. All other endpoints currently permit access; JWT authentication and role-based
 * authorisation ({@code ROLE_ADMIN}, {@code ROLE_CUSTOMER}) are introduced in Phase 1.
 */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
        "/actuator/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(PUBLIC_PATHS)
                                        .permitAll()
                                        // Phase 1 will replace this with authenticated() + JWT filter.
                                        .anyRequest()
                                        .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}