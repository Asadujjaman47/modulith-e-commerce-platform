package com.company.ecommerce.coupon.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.coupon.api.dto.CouponResponse;
import com.company.ecommerce.coupon.api.dto.CreateCouponRequest;
import com.company.ecommerce.coupon.application.CreateCouponUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin coupon management. Requires {@code ROLE_ADMIN}. */
@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Coupons", description = "Coupon management (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminCouponController {

    private final CreateCouponUseCase createCouponUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a discount coupon")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Coupon created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Coupon code already exists or rule violated")
    })
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CreateCouponRequest request) {
        return ApiResponse.success("Coupon created", createCouponUseCase.create(request));
    }
}
