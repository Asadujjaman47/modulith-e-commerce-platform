package com.company.ecommerce.review.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

/** Aggregate rating summary for a product. */
@Schema(description = "Aggregate product rating")
public record ProductRatingResponse(
        @Schema(description = "Product id") UUID productId,
        @Schema(description = "Average star rating", example = "4.50") BigDecimal averageRating,
        @Schema(description = "Number of published reviews", example = "12") int reviewCount) {}
