package com.company.ecommerce.coupon.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Request to validate a coupon against an order amount. */
@Schema(description = "Validate-coupon request")
public record ValidateCouponRequest(
        @Schema(description = "Coupon code", example = "SAVE20") @NotBlank @Size(max = 64)
                String code,
        @Schema(description = "Order amount to validate against", example = "150.00")
                @NotNull
                @Positive
                BigDecimal orderAmount) {}
