package com.company.ecommerce.review.api.dto;

import com.company.ecommerce.review.domain.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Admin request to change a review's moderation status. */
@Schema(description = "Update-review-status request")
public record UpdateReviewStatusRequest(
        @Schema(description = "New moderation status", example = "HIDDEN") @NotNull
                ReviewStatus status) {}
