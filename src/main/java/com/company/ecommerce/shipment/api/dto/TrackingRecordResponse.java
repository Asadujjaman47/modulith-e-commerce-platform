package com.company.ecommerce.shipment.api.dto;

import com.company.ecommerce.shipment.domain.ShipmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** A single entry in a shipment's tracking history. */
@Schema(description = "Tracking history entry")
public record TrackingRecordResponse(
        @Schema(description = "Tracking record id") UUID id,
        @Schema(description = "Status at this point", example = "IN_TRANSIT") ShipmentStatus status,
        @Schema(description = "Location", example = "Frankfurt hub") String location,
        @Schema(description = "Note", example = "Departed sorting facility") String note,
        @Schema(description = "When this update was recorded") Instant occurredAt) {}
