package com.company.ecommerce.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.company.ecommerce.audit.api.dto.AuditLogResponse;
import com.company.ecommerce.audit.domain.AuditCategory;
import com.company.ecommerce.audit.domain.AuditLog;
import com.company.ecommerce.audit.infrastructure.mapper.AuditLogMapper;
import com.company.ecommerce.audit.infrastructure.persistence.AuditLogRepository;
import com.company.ecommerce.common.api.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SearchAuditLogUseCaseTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AuditLogMapper auditLogMapper;
    @InjectMocks private SearchAuditLogUseCase useCase;

    @Test
    void appliesDefaultOccurredAtSortWhenUnsorted() {
        AuditLog log =
                AuditLog.record(
                        AuditCategory.ORDER,
                        "OrderCreated",
                        "CREATE",
                        "Order",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Order placed");
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));
        when(auditLogMapper.toResponse(log))
                .thenReturn(
                        new AuditLogResponse(
                                UUID.randomUUID(),
                                "ORDER",
                                "OrderCreated",
                                "CREATE",
                                "Order",
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "Order placed",
                                Instant.now()));

        AuditSearchCriteria criteria =
                new AuditSearchCriteria(AuditCategory.ORDER, null, null, null, null, null);
        PageResponse<AuditLogResponse> page = useCase.search(criteria, PageRequest.of(0, 20));

        assertThat(page.content()).hasSize(1);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(auditLogRepository)
                .findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("occurredAt")).isNotNull();
    }
}
