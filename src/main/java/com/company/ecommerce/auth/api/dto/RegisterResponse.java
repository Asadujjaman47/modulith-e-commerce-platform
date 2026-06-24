package com.company.ecommerce.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Result of a successful registration. */
@Schema(description = "Registration result")
public record RegisterResponse(
        @Schema(description = "Newly created user id") UUID userId,
        @Schema(description = "Registered email", example = "john@example.com") String email) {}