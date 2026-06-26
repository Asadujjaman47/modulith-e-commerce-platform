package com.company.ecommerce.audit.application;

import com.company.ecommerce.audit.infrastructure.mapper.AuditLogMapper;
import com.company.ecommerce.audit.infrastructure.persistence.AuditLogRepository;
import com.company.ecommerce.audit.infrastructure.persistence.AuditLogSpecifications;
import com.company.ecommerce.audit.api.dto.AuditLogResponse;
import com.company.ecommerce.common.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Searches the audit trail with optional filters, most-recent first. */
@Service
@RequiredArgsConstructor
public class SearchAuditLogUseCase {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "occurredAt");

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(AuditSearchCriteria criteria, Pageable pageable) {
        return PageResponse.from(
                auditLogRepository
                        .findAll(AuditLogSpecifications.matching(criteria), withDefaultSort(pageable))
                        .map(auditLogMapper::toResponse));
    }

    private static Pageable withDefaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
    }
}
