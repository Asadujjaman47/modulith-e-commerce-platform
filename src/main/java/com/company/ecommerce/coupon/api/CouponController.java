package com.company.ecommerce.coupon.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.coupon.api.dto.ApplyCouponRequest;
import com.company.ecommerce.coupon.api.dto.CouponApplicationResponse;
import com.company.ecommerce.coupon.api.dto.CouponValidationResponse;
import com.company.ecommerce.coupon.api.dto.ValidateCouponRequest;
import com.company.ecommerce.coupon.application.ApplyCouponUseCase;
import com.company.ecommerce.coupon.application.ValidateCouponUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated customer coupon endpoints: validate and apply discount codes. */
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Validate and apply discount coupons")
@SecurityRequirement(name = "bearerAuth")
public class CouponController {

    private final ValidateCouponUseCase validateCouponUseCase;
    private final ApplyCouponUseCase applyCouponUseCase;

    @PostMapping("/validate")
    @Operation(summary = "Validate a coupon against an order amount")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Coupon is valid; discount returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Coupon not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Coupon is not applicable (inactive, expired, below minimum, etc.)")
    })
    public ApiResponse<CouponValidationResponse> validate(
            @Valid @RequestBody ValidateCouponRequest request) {
        return ApiResponse.success(validateCouponUseCase.validate(request));
    }

    @PostMapping("/apply")
    @Operation(summary = "Apply a coupon to an order amount")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Coupon applied; discount recorded"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Coupon not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Coupon is not applicable")
    })
    public ApiResponse<CouponApplicationResponse> apply(
            @Valid @RequestBody ApplyCouponRequest request) {
        return ApiResponse.success("Coupon applied", applyCouponUseCase.apply(CurrentUser.id(), request));
    }
}
