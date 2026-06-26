package com.company.ecommerce.review.infrastructure.persistence;

import com.company.ecommerce.review.domain.ProductRating;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link ProductRating}. Internal to the {@code review} module. */
public interface ProductRatingRepository extends JpaRepository<ProductRating, UUID> {

    Optional<ProductRating> findByProductId(UUID productId);
}
