package com.company.ecommerce.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Customer profile representation returned to clients. */
@Schema(description = "Customer profile")
public record CustomerResponse(
        @Schema(description = "Customer id") UUID id,
        @Schema(description = "Email", example = "john@example.com") String email,
        @Schema(description = "Given name", example = "John") String firstName,
        @Schema(description = "Family name", example = "Doe") String lastName,
        @Schema(description = "Phone number", example = "+14155552671") String phone,
        @Schema(description = "When the profile was created") Instant createdAt) {}