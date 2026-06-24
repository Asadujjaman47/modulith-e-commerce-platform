package com.company.ecommerce.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Registration payload. */
@Schema(description = "New user registration request")
public record RegisterRequest(
        @Schema(description = "Email address", example = "john@example.com")
                @NotBlank
                @Email
                String email,
        @Schema(
                        description =
                                "Password: min 8 chars with upper, lower, digit and special character",
                        example = "Password123!")
                @NotBlank
                @Size(min = 8, max = 72)
                @Pattern(
                        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                        message =
                                "must contain an uppercase letter, a lowercase letter, a digit and a special character")
                String password,
        @Schema(description = "Given name", example = "John") @NotBlank @Size(max = 100)
                String firstName,
        @Schema(description = "Family name", example = "Doe") @NotBlank @Size(max = 100)
                String lastName) {}