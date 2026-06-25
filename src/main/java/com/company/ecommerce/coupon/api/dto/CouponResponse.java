package com.company.ecommerce.coupon.api.dto;

import com.company.ecommerce.coupon.domain.DiscountType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Coupon representation returned to clients. */
@Schema(description = "Discount coupon")
public record CouponResponse(
        @Schema(description = "Coupon id") UUID id,
        @Schema(description = "Coupon code", example = "SAVE20") String code,
        @Schema(description = "Description") String description,
        @Schema(description = "Discount type", example = "PERCENTAGE") DiscountType discountType,
        @Schema(description = "Discount value", example = "20") BigDecimal discountValue,
        @Schema(description = "Minimum order amount", example = "100.00") BigDecimal minOrderAmount,
        @Schema(description = "Maximum discount amount", example = "50.00")
                BigDecimal maxDiscountAmount,
        @Schema(description = "Start of validity window") Instant validFrom,
        @Schema(description = "End of validity window") Instant validUntil,
        @Schema(description = "Total allowed uses; null = unlimited", example = "1000")
                Integer usageLimit,
        @Schema(description = "Times the coupon has been used", example = "12") int timesUsed,
        @Schema(description = "Whether the coupon is active", example = "true") boolean active) {}
