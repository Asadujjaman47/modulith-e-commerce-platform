package com.company.ecommerce.audit.application;

import com.company.ecommerce.audit.domain.AuditCategory;
import java.time.Instant;
import java.util.UUID;

/**
 * Optional filters for an audit-log search. Any {@code null} field is ignored. {@code from}/{@code to}
 * bound the {@code occurredAt} timestamp (inclusive).
 */
public record AuditSearchCriteria(
        AuditCategory category,
        String eventType,
        UUID entityId,
        UUID actorId,
        Instant from,
        Instant to) {}
