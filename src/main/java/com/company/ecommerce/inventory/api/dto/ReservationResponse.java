package com.company.ecommerce.inventory.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Result of a stock reservation. */
@Schema(description = "Stock reservation")
public record ReservationResponse(
        @Schema(description = "Reservation id") UUID reservationId,
        @Schema(description = "Product id") UUID productId,
        @Schema(description = "Reserved quantity", example = "2") int quantity,
        @Schema(description = "Reservation status", example = "ACTIVE") String status) {}