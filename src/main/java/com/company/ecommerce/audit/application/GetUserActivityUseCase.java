package com.company.ecommerce.audit.application;

import com.company.ecommerce.audit.api.dto.AuditLogResponse;
import com.company.ecommerce.common.api.PageResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Returns the audit-trail activity for a single user (the acting customer), most-recent first. Backed
 * by the same trail as {@link SearchAuditLogUseCase}, filtered to one {@code actorId}.
 */
@Service
@RequiredArgsConstructor
public class GetUserActivityUseCase {

    private final SearchAuditLogUseCase searchAuditLogUseCase;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> forUser(UUID userId, Pageable pageable) {
        AuditSearchCriteria criteria =
                new AuditSearchCriteria(null, null, null, userId, null, null);
        return searchAuditLogUseCase.search(criteria, pageable);
    }
}
