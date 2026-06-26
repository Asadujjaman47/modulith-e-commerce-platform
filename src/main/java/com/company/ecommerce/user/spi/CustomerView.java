package com.company.ecommerce.user.spi;

import java.util.UUID;

/**
 * Lightweight, read-only projection of a customer for cross-module consumers (e.g. {@code review},
 * which snapshots the author's display name on a review). Carries only the fields other modules
 * legitimately need — no aggregate or JPA entity leaks across boundaries.
 */
public record CustomerView(UUID userId, String displayName, String email) {}
