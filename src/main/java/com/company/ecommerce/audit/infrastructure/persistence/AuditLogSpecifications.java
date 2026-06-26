package com.company.ecommerce.audit.infrastructure.persistence;

import com.company.ecommerce.audit.application.AuditSearchCriteria;
import com.company.ecommerce.audit.domain.AuditLog;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/** Builds dynamic {@link Specification}s for audit-log search. */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {}

    public static Specification<AuditLog> matching(AuditSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.category() != null) {
                predicates.add(cb.equal(root.get("category"), criteria.category()));
            }
            if (StringUtils.hasText(criteria.eventType())) {
                predicates.add(cb.equal(root.get("eventType"), criteria.eventType().trim()));
            }
            if (criteria.entityId() != null) {
                predicates.add(cb.equal(root.get("entityId"), criteria.entityId()));
            }
            if (criteria.actorId() != null) {
                predicates.add(cb.equal(root.get("actorId"), criteria.actorId()));
            }
            if (criteria.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), criteria.from()));
            }
            if (criteria.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), criteria.to()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
