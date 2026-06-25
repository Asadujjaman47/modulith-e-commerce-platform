package com.company.ecommerce.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Request to set a product's absolute on-hand quantity. */
@Schema(description = "Update-stock request")
public record UpdateStockRequest(
        @Schema(description = "New absolute on-hand quantity", example = "100")
                @PositiveOrZero
                int quantityOnHand,
        @Schema(description = "Reason for the adjustment", example = "Cycle count correction")
                @Size(max = 255)
                String reason) {}