package com.company.ecommerce.review.application;

import com.company.ecommerce.review.domain.ProductRating;
import com.company.ecommerce.review.infrastructure.persistence.ProductRatingRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Maintains the per-product aggregate rating as reviews are created, deleted or moderated. Keeps the
 * get-or-create and add/remove logic in one place so the use cases stay focused.
 */
@Service
@RequiredArgsConstructor
class ProductRatingService {

    private final ProductRatingRepository productRatingRepository;

    /** Adds a published review's rating to the product's aggregate, creating it if necessary. */
    void addRating(UUID productId, int rating) {
        ProductRating productRating =
                productRatingRepository
                        .findByProductId(productId)
                        .orElseGet(() -> ProductRating.empty(productId));
        productRating.addRating(rating);
        productRatingRepository.save(productRating);
    }

    /** Removes a previously-counted rating from the product's aggregate, if one exists. */
    void removeRating(UUID productId, int rating) {
        productRatingRepository
                .findByProductId(productId)
                .ifPresent(
                        productRating -> {
                            productRating.removeRating(rating);
                            productRatingRepository.save(productRating);
                        });
    }
}
