package com.company.ecommerce.auth.infrastructure.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalised CORS configuration bound from {@code app.cors.*}. Defaults are permissive enough for
 * local development; production overrides {@code allowed-origins} with the real front-end origins via
 * environment variables.
 *
 * @param allowedOrigins origins permitted to call the API (exact origins; no wildcard when
 *     credentials are allowed)
 * @param allowedMethods permitted HTTP methods
 * @param allowedHeaders permitted request headers
 * @param allowCredentials whether the browser may send credentials (cookies / Authorization)
 * @param maxAge how long (seconds) a preflight response may be cached
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        boolean allowCredentials,
        long maxAge) {

    public CorsProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:3000");
        }
        if (allowedMethods == null || allowedMethods.isEmpty()) {
            allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        }
        if (allowedHeaders == null || allowedHeaders.isEmpty()) {
            allowedHeaders = List.of("*");
        }
        if (maxAge <= 0) {
            maxAge = 3600;
        }
    }
}
