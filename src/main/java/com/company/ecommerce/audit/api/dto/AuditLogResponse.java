package com.company.ecommerce.audit.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** A single audit-trail entry. */
@Schema(description = "A single audit-trail entry")
public record AuditLogResponse(
        @Schema(description = "Audit entry id") UUID id,
        @Schema(description = "Functional area", example = "ORDER") String category,
        @Schema(description = "Event type", example = "OrderCreated") String eventType,
        @Schema(description = "Action performed", example = "CREATE") String action,
        @Schema(description = "Affected entity type", example = "Order") String entityType,
        @Schema(description = "Affected entity id") UUID entityId,
        @Schema(description = "Acting customer id, when applicable") UUID actorId,
        @Schema(description = "Human-readable summary", example = "Order ORD-1001 placed; total 49.99")
                String description,
        @Schema(description = "When the event occurred") Instant occurredAt) {}
