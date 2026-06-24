package com.company.ecommerce.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Refresh payload. */
@Schema(description = "Access-token refresh request")
public record RefreshTokenRequest(
        @Schema(description = "A valid, non-revoked refresh token") @NotBlank String refreshToken) {}