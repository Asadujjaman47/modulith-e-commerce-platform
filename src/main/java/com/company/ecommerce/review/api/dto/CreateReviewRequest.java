package com.company.ecommerce.review.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request to create a review for a product. */
@Schema(description = "Create-review request")
public record CreateReviewRequest(
        @Schema(description = "Star rating from 1 to 5", example = "5")
                @NotNull
                @Min(1)
                @Max(5)
                Integer rating,
        @Schema(description = "Short review title", example = "Great product")
                @Size(max = 150)
                String title,
        @Schema(description = "Review body", example = "Exactly as described, fast shipping.")
                @Size(max = 2000)
                String comment) {}
