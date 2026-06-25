package com.company.ecommerce.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Stock levels for a product. */
@Schema(description = "Product stock levels")
public record InventoryResponse(
        @Schema(description = "Product id") UUID productId,
        @Schema(description = "Units physically on hand", example = "100") int quantityOnHand,
        @Schema(description = "Units reserved against pending operations", example = "5")
                int quantityReserved,
        @Schema(description = "Units available to reserve (on-hand minus reserved)", example = "95")
                int quantityAvailable) {}