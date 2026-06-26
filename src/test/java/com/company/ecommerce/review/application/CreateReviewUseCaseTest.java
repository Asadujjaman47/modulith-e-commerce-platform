package com.company.ecommerce.review.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.catalog.spi.CatalogQuery;
import com.company.ecommerce.catalog.spi.ProductView;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.review.api.dto.CreateReviewRequest;
import com.company.ecommerce.review.domain.Review;
import com.company.ecommerce.review.domain.event.ReviewCreatedEvent;
import com.company.ecommerce.review.infrastructure.mapper.ReviewMapper;
import com.company.ecommerce.review.infrastructure.persistence.ReviewEligibilityRepository;
import com.company.ecommerce.review.infrastructure.persistence.ReviewRepository;
import com.company.ecommerce.user.spi.CustomerView;
import com.company.ecommerce.user.spi.UserQuery;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CreateReviewUseCaseTest {

    @Mock private CatalogQuery catalogQuery;
    @Mock private UserQuery userQuery;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ReviewEligibilityRepository eligibilityRepository;
    @Mock private ProductRatingService productRatingService;
    @Mock private ReviewMapper reviewMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private CreateReviewUseCase useCase;

    private final UUID productId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final CreateReviewRequest request = new CreateReviewRequest(5, "Great", "Loved it");

    private ProductView product(boolean active) {
        return new ProductView(productId, "Widget", "SKU-1", new BigDecimal("10.00"), active);
    }

    @Test
    void rejectsWhenProductMissing() {
        when(catalogQuery.findProduct(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.create(customerId, productId, request))
                .isInstanceOf(EntityNotFoundException.class);
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void rejectsWhenProductInactive() {
        when(catalogQuery.findProduct(productId)).thenReturn(Optional.of(product(false)));

        assertThatThrownBy(() -> useCase.create(customerId, productId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void rejectsWhenNotEligible() {
        when(catalogQuery.findProduct(productId)).thenReturn(Optional.of(product(true)));
        when(eligibilityRepository.existsByCustomerId(customerId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.create(customerId, productId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("completing an order");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateReview() {
        when(catalogQuery.findProduct(productId)).thenReturn(Optional.of(product(true)));
        when(eligibilityRepository.existsByCustomerId(customerId)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndCustomerId(productId, customerId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.create(customerId, productId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already reviewed");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createsReviewUpdatesRatingAndPublishesEvent() {
        when(catalogQuery.findProduct(productId)).thenReturn(Optional.of(product(true)));
        when(eligibilityRepository.existsByCustomerId(customerId)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndCustomerId(productId, customerId)).thenReturn(false);
        when(userQuery.findCustomer(customerId))
                .thenReturn(Optional.of(new CustomerView(customerId, "Jane Doe", "jane@example.com")));
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.create(customerId, productId, request);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getAuthorName())
                .isEqualTo("Jane Doe");

        verify(productRatingService).addRating(eq(productId), eq(5));

        ArgumentCaptor<ReviewCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(ReviewCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().productId())
                .isEqualTo(productId);
        assertThat(eventCaptor.getValue().rating()).isEqualTo(5);
    }

    @Test
    void fallsBackToAnonymousWhenCustomerUnknown() {
        when(catalogQuery.findProduct(productId)).thenReturn(Optional.of(product(true)));
        when(eligibilityRepository.existsByCustomerId(customerId)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndCustomerId(productId, customerId)).thenReturn(false);
        when(userQuery.findCustomer(customerId)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.create(customerId, productId, request);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getAuthorName())
                .isEqualTo("Anonymous");
        verify(productRatingService).addRating(eq(productId), anyInt());
    }
}
