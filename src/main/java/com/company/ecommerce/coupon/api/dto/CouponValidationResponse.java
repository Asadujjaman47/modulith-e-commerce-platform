package com.company.ecommerce.coupon.api.dto;

import com.company.ecommerce.coupon.domain.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Result of validating a coupon against an order amount. */
@Schema(description = "Coupon validation result")
public record CouponValidationResponse(
        @Schema(description = "Whether the coupon is valid for the order amount", example = "true")
                boolean valid,
        @Schema(description = "Coupon code", example = "SAVE20") String code,
        @Schema(description = "Discount type", example = "PERCENTAGE") DiscountType discountType,
        @Schema(description = "Order amount the coupon was validated against", example = "150.00")
                BigDecimal orderAmount,
        @Schema(description = "Computed discount amount", example = "30.00") BigDecimal discountAmount,
        @Schema(description = "Order amount after discount", example = "120.00")
                BigDecimal finalAmount) {}
