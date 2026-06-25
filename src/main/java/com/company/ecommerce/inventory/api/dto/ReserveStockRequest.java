package com.company.ecommerce.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Request to reserve stock for a product. */
@Schema(description = "Reserve-stock request")
public record ReserveStockRequest(
        @Schema(description = "Product to reserve stock for") @NotNull UUID productId,
        @Schema(description = "Number of units to reserve", example = "2") @Positive int quantity,
        @Schema(description = "Optional external reference (e.g. cart or order id)")
                @Size(max = 255)
                String reference) {}