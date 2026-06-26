package com.company.ecommerce.review.application;

import com.company.ecommerce.order.domain.event.OrderCompletedEvent;
import com.company.ecommerce.review.domain.ReviewEligibility;
import com.company.ecommerce.review.infrastructure.persistence.ReviewEligibilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Grants a customer review eligibility once they have a delivered order. Consumes the {@code order}
 * module's {@code events} named interface ({@code OrderCompletedEvent}) after the order transaction
 * commits, in its own transaction. Idempotent: a customer who already has at least one completed order
 * is left unchanged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewEligibilityHandler {

    private final ReviewEligibilityRepository eligibilityRepository;

    @ApplicationModuleListener
    public void on(OrderCompletedEvent event) {
        if (eligibilityRepository.existsByCustomerId(event.customerId())) {
            return;
        }
        eligibilityRepository.save(ReviewEligibility.of(event.customerId()));
        log.info("Customer {} is now eligible to write reviews.", event.customerId());
    }
}
