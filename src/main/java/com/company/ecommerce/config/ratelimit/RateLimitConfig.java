package com.company.ecommerce.config.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the rate-limiting subsystem when {@code app.rate-limit.enabled} is true (the default). The
 * filter is registered just after the Spring Security filter chain so the authenticated principal is
 * available for per-user keying, and scoped to {@code /api/*}.
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "app.rate-limit", name = "enabled", matchIfMissing = true)
public class RateLimitConfig {

    /**
     * Dedicated Lettuce client for Bucket4j (separate from Spring Data Redis). Created lazily — it
     * does not connect here, so a Redis outage does not block startup; the connection is attempted in
     * {@link RateLimitService}, which falls back to in-memory buckets on failure.
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimitRedisClient(RedisProperties props) {
        RedisURI.Builder uri =
                RedisURI.builder()
                        .withHost(props.getHost() != null ? props.getHost() : "localhost")
                        .withPort(props.getPort())
                        .withSsl(props.getSsl() != null && props.getSsl().isEnabled());
        if (props.getPassword() != null) {
            if (props.getUsername() != null) {
                uri.withAuthentication(props.getUsername(), props.getPassword().toCharArray());
            } else {
                uri.withPassword(props.getPassword().toCharArray());
            }
        }
        if (props.getDatabase() != 0) {
            uri.withDatabase(props.getDatabase());
        }
        if (props.getTimeout() != null) {
            uri.withTimeout(props.getTimeout());
        }
        return RedisClient.create(uri.build());
    }

    @Bean
    public RateLimitService rateLimitService(ObjectProvider<RedisClient> rateLimitRedisClient) {
        return new RateLimitService(rateLimitRedisClient);
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            RateLimitService rateLimitService,
            RateLimitProperties properties,
            ObjectMapper objectMapper) {
        var registration = new FilterRegistrationBean<RateLimitFilter>();
        registration.setFilter(new RateLimitFilter(rateLimitService, properties, objectMapper));
        registration.addUrlPatterns("/api/*");
        // Just after the Spring Security filter chain (DEFAULT_FILTER_ORDER = -100).
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER + 10);
        return registration;
    }
}
