package com.company.ecommerce.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Logout payload: the refresh token to revoke. */
@Schema(description = "Logout request")
public record LogoutRequest(
        @Schema(description = "The refresh token to revoke") @NotBlank String refreshToken) {}