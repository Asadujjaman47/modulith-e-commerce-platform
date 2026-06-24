package com.company.ecommerce.user.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.user.api.dto.CustomerResponse;
import com.company.ecommerce.user.api.dto.UpdateProfileRequest;
import com.company.ecommerce.user.application.GetCustomerUseCase;
import com.company.ecommerce.user.application.UpdateCustomerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated customer profile endpoints. */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Authenticated customer profile management")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final GetCustomerUseCase getCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated customer's profile")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Profile returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Profile not found")
    })
    public ApiResponse<CustomerResponse> getProfile() {
        return ApiResponse.success(getCustomerUseCase.getByUserId(CurrentUser.id()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated customer's profile")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Profile updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token")
    })
    public ApiResponse<CustomerResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(
                "Profile updated", updateCustomerUseCase.update(CurrentUser.id(), request));
    }
}