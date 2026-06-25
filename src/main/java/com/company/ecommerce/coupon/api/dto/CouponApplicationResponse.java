package com.company.ecommerce.coupon.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

/** Result of applying a coupon: the recorded discount and resulting amount. */
@Schema(description = "Coupon application result")
public record CouponApplicationResponse(
        @Schema(description = "Applied coupon id") UUID couponId,
        @Schema(description = "Coupon code", example = "SAVE20") String code,
        @Schema(description = "Original order amount", example = "150.00") BigDecimal orderAmount,
        @Schema(description = "Discount applied", example = "30.00") BigDecimal discountAmount,
        @Schema(description = "Amount payable after discount", example = "120.00")
                BigDecimal finalAmount) {}
