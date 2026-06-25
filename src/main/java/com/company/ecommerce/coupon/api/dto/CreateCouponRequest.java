package com.company.ecommerce.coupon.api.dto;

import com.company.ecommerce.coupon.domain.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/** Request to create a discount coupon (admin). */
@Schema(description = "Create-coupon request")
public record CreateCouponRequest(
        @Schema(description = "Unique coupon code", example = "SAVE20")
                @NotBlank
                @Size(max = 64)
                String code,
        @Schema(description = "Human-readable description", example = "20% off orders over $100")
                @Size(max = 255)
                String description,
        @Schema(description = "Discount type", example = "PERCENTAGE") @NotNull
                DiscountType discountType,
        @Schema(description = "Discount value (percentage 0–100, or fixed amount)", example = "20")
                @NotNull
                @Positive
                BigDecimal discountValue,
        @Schema(description = "Minimum order amount to qualify", example = "100.00")
                @PositiveOrZero
                BigDecimal minOrderAmount,
        @Schema(description = "Maximum discount for percentage coupons", example = "50.00")
                @Positive
                BigDecimal maxDiscountAmount,
        @Schema(description = "Start of validity window") @NotNull Instant validFrom,
        @Schema(description = "End of validity window") @NotNull Instant validUntil,
        @Schema(description = "Total number of allowed uses; null = unlimited", example = "1000")
                @Positive
                Integer usageLimit) {}
