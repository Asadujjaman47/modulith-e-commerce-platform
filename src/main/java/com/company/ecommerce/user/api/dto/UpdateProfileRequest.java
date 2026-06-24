package com.company.ecommerce.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Profile update payload. */
@Schema(description = "Customer profile update request")
public record UpdateProfileRequest(
        @Schema(description = "Given name", example = "John") @NotBlank @Size(max = 100)
                String firstName,
        @Schema(description = "Family name", example = "Doe") @NotBlank @Size(max = 100)
                String lastName,
        @Schema(description = "Phone number (E.164)", example = "+14155552671")
                @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "must be a valid phone number")
                @Size(max = 20)
                String phone) {}