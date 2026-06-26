package com.company.ecommerce.review.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.order.domain.event.OrderCompletedEvent;
import com.company.ecommerce.review.domain.ReviewEligibility;
import com.company.ecommerce.review.infrastructure.persistence.ReviewEligibilityRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewEligibilityHandlerTest {

    @Mock private ReviewEligibilityRepository eligibilityRepository;
    @InjectMocks private ReviewEligibilityHandler handler;

    private final UUID customerId = UUID.randomUUID();

    @Test
    void grantsEligibilityOnFirstCompletedOrder() {
        when(eligibilityRepository.existsByCustomerId(customerId)).thenReturn(false);

        handler.on(new OrderCompletedEvent(UUID.randomUUID(), customerId));

        verify(eligibilityRepository).save(any(ReviewEligibility.class));
    }

    @Test
    void isIdempotentWhenAlreadyEligible() {
        when(eligibilityRepository.existsByCustomerId(customerId)).thenReturn(true);

        handler.on(new OrderCompletedEvent(UUID.randomUUID(), customerId));

        verify(eligibilityRepository, never()).save(any());
    }
}
