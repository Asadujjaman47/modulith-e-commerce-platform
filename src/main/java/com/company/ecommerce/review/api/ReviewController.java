package com.company.ecommerce.review.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.common.api.PageResponse;
import com.company.ecommerce.review.api.dto.CreateReviewRequest;
import com.company.ecommerce.review.api.dto.ProductRatingResponse;
import com.company.ecommerce.review.api.dto.ReviewResponse;
import com.company.ecommerce.review.application.CreateReviewUseCase;
import com.company.ecommerce.review.application.DeleteReviewUseCase;
import com.company.ecommerce.review.application.GetReviewsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated product-review endpoints: browse a product's reviews, post one, delete your own. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews and ratings")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final GetReviewsUseCase getReviewsUseCase;
    private final CreateReviewUseCase createReviewUseCase;
    private final DeleteReviewUseCase deleteReviewUseCase;

    @GetMapping("/products/{productId}/reviews")
    @Operation(summary = "List published reviews for a product")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Reviews returned"))
    public ApiResponse<PageResponse<ReviewResponse>> list(
            @PathVariable UUID productId, @ParameterObject Pageable pageable) {
        return ApiResponse.success(getReviewsUseCase.listForProduct(productId, pageable));
    }

    @GetMapping("/products/{productId}/reviews/summary")
    @Operation(summary = "Get a product's aggregate rating summary")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rating summary returned"))
    public ApiResponse<ProductRatingResponse> summary(@PathVariable UUID productId) {
        return ApiResponse.success(getReviewsUseCase.getRatingSummary(productId));
    }

    @PostMapping("/products/{productId}/reviews")
    @Operation(
            summary = "Create a review for a product",
            description =
                    "Requires the customer to have completed at least one order. A customer may review a"
                            + " product only once.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Review created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Product not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Not eligible, product inactive, or already reviewed")
    })
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @PathVariable UUID productId, @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse review = createReviewUseCase.create(CurrentUser.id(), productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review created", review));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @Operation(summary = "Delete your own review")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Review deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Review not found")
    })
    public ApiResponse<Void> delete(@PathVariable UUID reviewId) {
        deleteReviewUseCase.deleteOwn(CurrentUser.id(), reviewId);
        return ApiResponse.success("Review deleted", null);
    }
}
