package com.company.ecommerce.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Issued tokens returned on login and refresh. */
@Schema(description = "Issued authentication tokens")
public record TokenResponse(
        @Schema(description = "Short-lived JWT access token") String accessToken,
        @Schema(description = "Long-lived opaque refresh token") String refreshToken,
        @Schema(description = "Access token lifetime in seconds", example = "900") long expiresIn,
        @Schema(description = "Token type", example = "Bearer") String tokenType) {

    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, expiresIn, "Bearer");
    }
}