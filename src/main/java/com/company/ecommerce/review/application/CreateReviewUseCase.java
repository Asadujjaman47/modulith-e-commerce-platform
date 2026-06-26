package com.company.ecommerce.review.application;

import com.company.ecommerce.catalog.spi.CatalogQuery;
import com.company.ecommerce.catalog.spi.ProductView;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.review.api.dto.CreateReviewRequest;
import com.company.ecommerce.review.api.dto.ReviewResponse;
import com.company.ecommerce.review.domain.Review;
import com.company.ecommerce.review.domain.event.ReviewCreatedEvent;
import com.company.ecommerce.review.infrastructure.mapper.ReviewMapper;
import com.company.ecommerce.review.infrastructure.persistence.ReviewEligibilityRepository;
import com.company.ecommerce.review.infrastructure.persistence.ReviewRepository;
import com.company.ecommerce.user.spi.CustomerView;
import com.company.ecommerce.user.spi.UserQuery;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a product review for an authenticated, purchase-eligible customer.
 *
 * <p>Validates the product exists and is active (via the catalog {@code spi}), enforces the
 * purchase gate (the customer must have at least one completed order — recorded in the local
 * eligibility replica) and one-review-per-product, snapshots the author's display name (via the user
 * {@code spi}), updates the product's aggregate rating and publishes {@link ReviewCreatedEvent}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateReviewUseCase {

    private static final String ANONYMOUS = "Anonymous";

    private final CatalogQuery catalogQuery;
    private final UserQuery userQuery;
    private final ReviewRepository reviewRepository;
    private final ReviewEligibilityRepository eligibilityRepository;
    private final ProductRatingService productRatingService;
    private final ReviewMapper reviewMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReviewResponse create(UUID customerId, UUID productId, CreateReviewRequest request) {
        ProductView product =
                catalogQuery
                        .findProduct(productId)
                        .orElseThrow(() -> new EntityNotFoundException("Product", productId));
        if (!product.active()) {
            throw new BusinessException("Product %s is not available for review".formatted(productId));
        }
        if (!eligibilityRepository.existsByCustomerId(customerId)) {
            throw new BusinessException(
                    "You can only review products after completing an order");
        }
        if (reviewRepository.existsByProductIdAndCustomerId(productId, customerId)) {
            throw new BusinessException("You have already reviewed this product");
        }

        String authorName =
                userQuery
                        .findCustomer(customerId)
                        .map(CustomerView::displayName)
                        .filter(name -> !name.isBlank())
                        .orElse(ANONYMOUS);

        Review review =
                reviewRepository.save(
                        Review.create(
                                productId,
                                customerId,
                                authorName,
                                request.rating(),
                                request.title(),
                                request.comment()));
        productRatingService.addRating(productId, review.getRating());

        eventPublisher.publishEvent(
                new ReviewCreatedEvent(review.getId(), productId, customerId, review.getRating()));
        log.info(
                "Review created. reviewId={} productId={} customerId={} rating={}",
                review.getId(),
                productId,
                customerId,
                review.getRating());
        return reviewMapper.toResponse(review);
    }
}
