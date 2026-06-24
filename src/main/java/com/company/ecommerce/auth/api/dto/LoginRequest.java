package com.company.ecommerce.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Login payload. */
@Schema(description = "Login request")
public record LoginRequest(
        @Schema(description = "Email address", example = "john@example.com") @NotBlank @Email
                String email,
        @Schema(description = "Password", example = "Password123!") @NotBlank String password) {}