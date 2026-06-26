package com.company.ecommerce.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.ecommerce.review.api.dto.ProductRatingResponse;
import com.company.ecommerce.review.domain.ProductRating;
import com.company.ecommerce.review.infrastructure.mapper.ReviewMapper;
import com.company.ecommerce.review.infrastructure.persistence.ProductRatingRepository;
import com.company.ecommerce.review.infrastructure.persistence.ReviewRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetReviewsUseCaseTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRatingRepository productRatingRepository;
    @Mock private ReviewMapper reviewMapper;
    @InjectMocks private GetReviewsUseCase useCase;

    private final UUID productId = UUID.randomUUID();

    @Test
    void summaryReturnsZerosWhenProductHasNoReviews() {
        when(productRatingRepository.findByProductId(productId)).thenReturn(Optional.empty());

        ProductRatingResponse summary = useCase.getRatingSummary(productId);

        assertThat(summary.productId()).isEqualTo(productId);
        assertThat(summary.reviewCount()).isZero();
        assertThat(summary.averageRating()).isEqualByComparingTo("0.00");
    }

    @Test
    void summaryMapsExistingRating() {
        ProductRating rating = ProductRating.empty(productId);
        rating.addRating(4);
        when(productRatingRepository.findByProductId(productId)).thenReturn(Optional.of(rating));
        when(reviewMapper.toRatingResponse(rating))
                .thenReturn(new ProductRatingResponse(productId, rating.getAverageRating(), 1));

        ProductRatingResponse summary = useCase.getRatingSummary(productId);

        assertThat(summary.reviewCount()).isEqualTo(1);
        assertThat(summary.averageRating()).isEqualByComparingTo("4.00");
    }
}
