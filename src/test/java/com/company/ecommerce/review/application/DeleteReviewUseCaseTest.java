package com.company.ecommerce.review.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.review.domain.Review;
import com.company.ecommerce.review.infrastructure.persistence.ReviewRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteReviewUseCaseTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRatingService productRatingService;
    @InjectMocks private DeleteReviewUseCase useCase;

    private final UUID reviewId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    private Review publishedReview() {
        return Review.create(productId, customerId, "Jane", 4, "t", "c");
    }

    @Test
    void rejectsWhenReviewNotFoundForCustomer() {
        when(reviewRepository.findByIdAndCustomerId(reviewId, customerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.deleteOwn(customerId, reviewId))
                .isInstanceOf(EntityNotFoundException.class);
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void deletesPublishedReviewAndDecrementsRating() {
        Review review = publishedReview();
        when(reviewRepository.findByIdAndCustomerId(reviewId, customerId))
                .thenReturn(Optional.of(review));

        useCase.deleteOwn(customerId, reviewId);

        verify(productRatingService).removeRating(eq(productId), eq(4));
        verify(reviewRepository).delete(review);
    }

    @Test
    void doesNotTouchRatingForHiddenReview() {
        Review review = publishedReview();
        review.hide();
        when(reviewRepository.findByIdAndCustomerId(reviewId, customerId))
                .thenReturn(Optional.of(review));

        useCase.deleteOwn(customerId, reviewId);

        verify(productRatingService, never()).removeRating(any(), org.mockito.ArgumentMatchers.anyInt());
        verify(reviewRepository).delete(review);
    }
}
