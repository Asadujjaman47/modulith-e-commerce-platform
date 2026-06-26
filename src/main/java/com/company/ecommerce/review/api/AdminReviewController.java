package com.company.ecommerce.review.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.review.api.dto.ReviewResponse;
import com.company.ecommerce.review.api.dto.UpdateReviewStatusRequest;
import com.company.ecommerce.review.application.ModerateReviewUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin review moderation: hide or restore a review. */
@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Reviews", description = "Review moderation (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminReviewController {

    private final ModerateReviewUseCase moderateReviewUseCase;

    @PutMapping("/{reviewId}/status")
    @Operation(
            summary = "Moderate a review",
            description = "Hide a published review or restore a hidden one; keeps the rating in sync.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Review status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Caller is not an admin"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Review not found")
    })
    public ApiResponse<ReviewResponse> updateStatus(
            @PathVariable UUID reviewId, @Valid @RequestBody UpdateReviewStatusRequest request) {
        return ApiResponse.success(
                "Review status updated",
                moderateReviewUseCase.updateStatus(reviewId, request.status()));
    }
}
