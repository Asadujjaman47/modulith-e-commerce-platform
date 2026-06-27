package com.company.ecommerce.auth.infrastructure.security;

import com.company.ecommerce.common.api.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Application security configuration. Owned by the {@code auth} module, which is responsible for
 * authentication and authorization across the platform.
 *
 * <p>The API is stateless and protected by JWT. Registration, login, refresh, actuator and OpenAPI
 * endpoints are public; everything else requires a valid access token. Method-level security
 * ({@code @PreAuthorize}) is enabled for role checks.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

    /** Endpoints reachable without authentication. */
    private static final String[] PUBLIC_PATHS = {
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    /** Actuator endpoints that are always public (probes and build info). */
    private static final String[] PUBLIC_ACTUATOR_PATHS = {
        "/actuator/health", "/actuator/health/**", "/actuator/info"
    };

    private static final long HSTS_MAX_AGE_SECONDS = 31_536_000L; // 1 year

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(
                        headers ->
                                headers.frameOptions(frame -> frame.deny())
                                        .contentTypeOptions(Customizer.withDefaults())
                                        .httpStrictTransportSecurity(
                                                hsts ->
                                                        hsts.includeSubDomains(true)
                                                                .maxAgeInSeconds(HSTS_MAX_AGE_SECONDS))
                                        .referrerPolicy(
                                                rp ->
                                                        rp.policy(
                                                                ReferrerPolicy
                                                                        .STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(PUBLIC_PATHS)
                                        .permitAll()
                                        .requestMatchers(PUBLIC_ACTUATOR_PATHS)
                                        .permitAll()
                                        // Metrics/Prometheus/Modulith actuators expose internal
                                        // detail and are restricted to admins (scraped with an
                                        // admin token).
                                        .requestMatchers("/actuator/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(authenticationEntryPoint())
                                        .accessDeniedHandler(accessDeniedHandler()))
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(corsProperties.allowedMethods());
        config.setAllowedHeaders(corsProperties.allowedHeaders());
        config.setAllowCredentials(corsProperties.allowCredentials());
        config.setMaxAge(corsProperties.maxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Returns 401 with the standard error envelope when authentication is missing/invalid. */
    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) ->
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
    }

    /** Returns 403 with the standard error envelope when the user lacks permission. */
    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) ->
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied");
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(message));
    }
}