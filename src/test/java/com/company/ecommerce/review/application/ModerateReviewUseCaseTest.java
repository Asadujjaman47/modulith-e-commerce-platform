package com.company.ecommerce.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.review.domain.Review;
import com.company.ecommerce.review.domain.ReviewStatus;
import com.company.ecommerce.review.infrastructure.mapper.ReviewMapper;
import com.company.ecommerce.review.infrastructure.persistence.ReviewRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModerateReviewUseCaseTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ProductRatingService productRatingService;
    @Mock private ReviewMapper reviewMapper;
    @InjectMocks private ModerateReviewUseCase useCase;

    private final UUID reviewId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    private Review review() {
        return Review.create(productId, UUID.randomUUID(), "Jane", 4, "t", "c");
    }

    @Test
    void hidingPublishedReviewRemovesItFromRating() {
        Review review = review();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        useCase.updateStatus(reviewId, ReviewStatus.HIDDEN);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        verify(productRatingService).removeRating(eq(productId), eq(4));
        verify(reviewRepository).save(review);
    }

    @Test
    void restoringHiddenReviewAddsItBackToRating() {
        Review review = review();
        review.hide();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        useCase.updateStatus(reviewId, ReviewStatus.PUBLISHED);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
        verify(productRatingService).addRating(eq(productId), eq(4));
    }

    @Test
    void noOpTransitionLeavesRatingUntouched() {
        Review review = review(); // already PUBLISHED
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        useCase.updateStatus(reviewId, ReviewStatus.PUBLISHED);

        verify(productRatingService, never()).addRating(eq(productId), anyInt());
        verify(productRatingService, never()).removeRating(eq(productId), anyInt());
    }
}
