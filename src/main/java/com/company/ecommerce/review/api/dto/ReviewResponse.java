package com.company.ecommerce.review.api.dto;

import com.company.ecommerce.review.domain.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Representation of a product review. */
@Schema(description = "Product review")
public record ReviewResponse(
        @Schema(description = "Review id") UUID id,
        @Schema(description = "Reviewed product id") UUID productId,
        @Schema(description = "Authoring customer id") UUID customerId,
        @Schema(description = "Author display name", example = "Jane Doe") String authorName,
        @Schema(description = "Star rating from 1 to 5", example = "5") int rating,
        @Schema(description = "Review title", example = "Great product") String title,
        @Schema(description = "Review body") String comment,
        @Schema(description = "Moderation status", example = "PUBLISHED") ReviewStatus status,
        @Schema(description = "When the review was created") Instant createdAt) {}
