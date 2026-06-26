package com.company.ecommerce.audit.application;

import com.company.ecommerce.audit.domain.AuditCategory;
import com.company.ecommerce.audit.domain.AuditLog;
import com.company.ecommerce.audit.infrastructure.persistence.AuditLogRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Single write path for the audit trail. The event handlers translate each business event into a call
 * here; they run on {@code @ApplicationModuleListener}s (post-commit, each in its own transaction), so
 * a failure to record an audit entry never disrupts the originating business flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogWriter {

    private final AuditLogRepository auditLogRepository;

    public void record(
            AuditCategory category,
            String eventType,
            String action,
            String entityType,
            UUID entityId,
            UUID actorId,
            String description) {
        auditLogRepository.save(
                AuditLog.record(
                        category, eventType, action, entityType, entityId, actorId, description));
        log.debug("Audit recorded: {} {} entity={} actor={}", category, eventType, entityId, actorId);
    }
}
