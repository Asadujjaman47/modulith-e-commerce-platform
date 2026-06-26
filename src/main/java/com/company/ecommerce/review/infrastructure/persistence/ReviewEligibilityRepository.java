package com.company.ecommerce.review.infrastructure.persistence;

import com.company.ecommerce.review.domain.ReviewEligibility;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link ReviewEligibility}. Internal to the {@code review} module. */
public interface ReviewEligibilityRepository extends JpaRepository<ReviewEligibility, UUID> {

    boolean existsByCustomerId(UUID customerId);
}
