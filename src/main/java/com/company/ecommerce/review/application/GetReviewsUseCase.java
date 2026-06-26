package com.company.ecommerce.review.application;

import com.company.ecommerce.common.api.PageResponse;
import com.company.ecommerce.review.api.dto.ProductRatingResponse;
import com.company.ecommerce.review.api.dto.ReviewResponse;
import com.company.ecommerce.review.domain.ReviewStatus;
import com.company.ecommerce.review.infrastructure.mapper.ReviewMapper;
import com.company.ecommerce.review.infrastructure.persistence.ProductRatingRepository;
import com.company.ecommerce.review.infrastructure.persistence.ReviewRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads published reviews and the aggregate rating for a product. */
@Service
@RequiredArgsConstructor
public class GetReviewsUseCase {

    private final ReviewRepository reviewRepository;
    private final ProductRatingRepository productRatingRepository;
    private final ReviewMapper reviewMapper;

    /** Returns the published reviews for a product, paginated. */
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listForProduct(UUID productId, Pageable pageable) {
        return PageResponse.from(
                reviewRepository
                        .findByProductIdAndStatus(productId, ReviewStatus.PUBLISHED, pageable)
                        .map(reviewMapper::toResponse));
    }

    /** Returns the aggregate rating summary for a product (zeros if it has no reviews yet). */
    @Transactional(readOnly = true)
    public ProductRatingResponse getRatingSummary(UUID productId) {
        return productRatingRepository
                .findByProductId(productId)
                .map(reviewMapper::toRatingResponse)
                .orElseGet(
                        () ->
                                new ProductRatingResponse(
                                        productId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0));
    }
}
