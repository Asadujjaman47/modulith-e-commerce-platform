package com.company.ecommerce.shipment.api.dto;

import com.company.ecommerce.shipment.domain.ShipmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin request to advance a shipment to the next status, with optional tracking detail. */
@Schema(description = "Update-shipment-status request")
public record UpdateShipmentStatusRequest(
        @Schema(description = "Target status", example = "IN_TRANSIT") @NotNull
                ShipmentStatus status,
        @Schema(description = "Optional current location", example = "Frankfurt hub")
                @Size(max = 255)
                String location,
        @Schema(description = "Optional note", example = "Departed sorting facility")
                @Size(max = 500)
                String note) {}
