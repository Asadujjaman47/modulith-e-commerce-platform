package com.company.ecommerce.audit.infrastructure.persistence;

import com.company.ecommerce.audit.domain.AuditLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Persistence for {@link AuditLog} entries. Extends {@link JpaSpecificationExecutor} to support the
 * dynamic, paginated audit-log search and per-user activity queries.
 */
public interface AuditLogRepository
        extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {}
