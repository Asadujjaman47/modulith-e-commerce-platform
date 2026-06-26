package com.company.ecommerce.review.infrastructure.mapper;

import com.company.ecommerce.review.api.dto.ProductRatingResponse;
import com.company.ecommerce.review.api.dto.ReviewResponse;
import com.company.ecommerce.review.domain.ProductRating;
import com.company.ecommerce.review.domain.Review;
import org.mapstruct.Mapper;

/** Maps review aggregates to response DTOs. */
@Mapper(componentModel = "spring")
public interface ReviewMapper {

    ReviewResponse toResponse(Review review);

    ProductRatingResponse toRatingResponse(ProductRating rating);
}
