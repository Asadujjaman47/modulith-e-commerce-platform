package com.company.ecommerce.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Enables Spring Data JPA auditing so {@code AuditableEntity} audit fields are populated
 * automatically.
 *
 * <p>The auditor is resolved from the security context. Until authentication is wired up
 * (Phase 1) requests are attributed to {@code "system"}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    private static final String SYSTEM = "system";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of(SYSTEM);
            }
            return Optional.of(authentication.getName());
        };
    }
}
