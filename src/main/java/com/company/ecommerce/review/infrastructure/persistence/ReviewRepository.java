package com.company.ecommerce.review.infrastructure.persistence;

import com.company.ecommerce.review.domain.Review;
import com.company.ecommerce.review.domain.ReviewStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Review}. Internal to the {@code review} module. */
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status, Pageable pageable);

    boolean existsByProductIdAndCustomerId(UUID productId, UUID customerId);

    Optional<Review> findByIdAndCustomerId(UUID id, UUID customerId);
}
