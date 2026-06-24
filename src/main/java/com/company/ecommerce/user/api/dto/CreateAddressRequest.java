package com.company.ecommerce.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Address creation payload. */
@Schema(description = "Create address request")
public record CreateAddressRequest(
        @Schema(description = "Label", example = "Home") @Size(max = 50) String label,
        @Schema(description = "Address line 1", example = "221B Baker Street") @NotBlank @Size(max = 200)
                String line1,
        @Schema(description = "Address line 2", example = "Apt 4") @Size(max = 200) String line2,
        @Schema(description = "City", example = "London") @NotBlank @Size(max = 100) String city,
        @Schema(description = "State / region", example = "Greater London") @Size(max = 100)
                String state,
        @Schema(description = "Postal code", example = "NW1 6XE") @NotBlank @Size(max = 20)
                String postalCode,
        @Schema(description = "ISO country", example = "GB") @NotBlank @Size(max = 100) String country,
        @Schema(description = "Mark as the default address", example = "true") boolean defaultAddress) {}