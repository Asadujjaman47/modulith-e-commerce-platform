package com.company.ecommerce.review.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Marks a customer as eligible to write reviews. Owned by the {@code review} module and populated from
 * {@code OrderCompletedEvent}: a customer becomes eligible once they have at least one delivered order.
 * References the customer by id value only — no cross-module FK.
 */
@Entity
@Table(name = "review_eligibility")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewEligibility extends AuditableEntity {

    @Column(name = "customer_id", nullable = false, unique = true)
    private UUID customerId;

    private ReviewEligibility(UUID customerId) {
        this.customerId = customerId;
    }

    public static ReviewEligibility of(UUID customerId) {
        return new ReviewEligibility(customerId);
    }
}
