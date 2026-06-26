package com.company.ecommerce.review.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.review.api.dto.ReviewResponse;
import com.company.ecommerce.review.domain.Review;
import com.company.ecommerce.review.domain.ReviewStatus;
import com.company.ecommerce.review.infrastructure.mapper.ReviewMapper;
import com.company.ecommerce.review.infrastructure.persistence.ReviewRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin moderation: hide a published review or restore a hidden one. The product's aggregate rating is
 * kept in sync — a hidden review is excluded from it, and a restored review re-counted. Idempotent: a
 * no-op transition leaves the rating untouched.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModerateReviewUseCase {

    private final ReviewRepository reviewRepository;
    private final ProductRatingService productRatingService;
    private final ReviewMapper reviewMapper;

    @Transactional
    public ReviewResponse updateStatus(UUID reviewId, ReviewStatus targetStatus) {
        Review review =
                reviewRepository
                        .findById(reviewId)
                        .orElseThrow(() -> new EntityNotFoundException("Review", reviewId));

        if (targetStatus == ReviewStatus.HIDDEN && review.isPublished()) {
            review.hide();
            productRatingService.removeRating(review.getProductId(), review.getRating());
            log.info("Review hidden. reviewId={}", reviewId);
        } else if (targetStatus == ReviewStatus.PUBLISHED && !review.isPublished()) {
            review.publish();
            productRatingService.addRating(review.getProductId(), review.getRating());
            log.info("Review restored. reviewId={}", reviewId);
        }
        reviewRepository.save(review);
        return reviewMapper.toResponse(review);
    }
}
