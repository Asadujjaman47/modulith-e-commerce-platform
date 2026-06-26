package com.company.ecommerce.audit.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Append-only audit trail entry. Owned by the {@code audit} module.
 *
 * <p>One row is written per observed business event. The entry is immutable once recorded. The
 * affected entity and the acting customer are referenced by id value only — no cross-module FKs.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private AuditCategory category;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    private AuditLog(
            AuditCategory category,
            String eventType,
            String action,
            String entityType,
            UUID entityId,
            UUID actorId,
            String description) {
        this.category = category;
        this.eventType = eventType;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.actorId = actorId;
        this.description = description;
        this.occurredAt = Instant.now();
    }

    /** Records an audit entry for an observed business event. */
    public static AuditLog record(
            AuditCategory category,
            String eventType,
            String action,
            String entityType,
            UUID entityId,
            UUID actorId,
            String description) {
        return new AuditLog(category, eventType, action, entityType, entityId, actorId, description);
    }
}
