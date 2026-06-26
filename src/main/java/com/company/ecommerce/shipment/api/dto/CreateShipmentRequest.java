package com.company.ecommerce.shipment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Admin request to create a shipment for a paid order. */
@Schema(description = "Create-shipment request")
public record CreateShipmentRequest(
        @Schema(
                        description = "Id of the paid order to ship",
                        example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                @NotNull
                UUID orderId,
        @Schema(description = "Carrier handling the delivery", example = "DHL")
                @NotBlank
                @Size(max = 100)
                String carrier) {}
