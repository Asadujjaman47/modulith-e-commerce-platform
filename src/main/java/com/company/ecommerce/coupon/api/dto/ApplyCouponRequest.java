package com.company.ecommerce.coupon.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Request to apply a coupon to an order amount on behalf of the authenticated customer. */
@Schema(description = "Apply-coupon request")
public record ApplyCouponRequest(
        @Schema(description = "Coupon code", example = "SAVE20") @NotBlank @Size(max = 64)
                String code,
        @Schema(description = "Order amount to apply the coupon to", example = "150.00")
                @NotNull
                @Positive
                BigDecimal orderAmount) {}
