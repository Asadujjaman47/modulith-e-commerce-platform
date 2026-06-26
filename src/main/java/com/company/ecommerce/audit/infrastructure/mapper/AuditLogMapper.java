package com.company.ecommerce.audit.infrastructure.mapper;

import com.company.ecommerce.audit.api.dto.AuditLogResponse;
import com.company.ecommerce.audit.domain.AuditLog;
import org.mapstruct.Mapper;

/** Maps {@link AuditLog} entries to response DTOs (the {@code category} enum maps to its name). */
@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog auditLog);
}
