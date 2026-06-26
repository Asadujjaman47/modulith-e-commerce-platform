package com.company.ecommerce.review.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.review.domain.Review;
import com.company.ecommerce.review.infrastructure.persistence.ReviewRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deletes a customer's own review and updates the product's aggregate rating. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteReviewUseCase {

    private final ReviewRepository reviewRepository;
    private final ProductRatingService productRatingService;

    @Transactional
    public void deleteOwn(UUID customerId, UUID reviewId) {
        Review review =
                reviewRepository
                        .findByIdAndCustomerId(reviewId, customerId)
                        .orElseThrow(() -> new EntityNotFoundException("Review", reviewId));
        if (review.isPublished()) {
            productRatingService.removeRating(review.getProductId(), review.getRating());
        }
        reviewRepository.delete(review);
        log.info("Review deleted. reviewId={} customerId={}", reviewId, customerId);
    }
}
