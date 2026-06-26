package com.company.ecommerce.review.domain.event;

import java.util.UUID;

/**
 * Published when a customer creates a product review.
 *
 * <p>Consumed in later phases by {@code reporting}/{@code audit}. Carries the review id, the reviewed
 * product, the authoring customer and the star rating.
 */
public record ReviewCreatedEvent(UUID reviewId, UUID productId, UUID customerId, int rating) {}
