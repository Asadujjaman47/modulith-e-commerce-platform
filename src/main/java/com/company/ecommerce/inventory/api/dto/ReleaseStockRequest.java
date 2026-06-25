package com.company.ecommerce.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request to release a previously created stock reservation. */
@Schema(description = "Release-stock request")
public record ReleaseStockRequest(
        @Schema(description = "Reservation to release") @NotNull UUID reservationId) {}