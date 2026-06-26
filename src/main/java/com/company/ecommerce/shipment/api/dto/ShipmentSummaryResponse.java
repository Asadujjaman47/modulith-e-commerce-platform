package com.company.ecommerce.shipment.api.dto;

import com.company.ecommerce.shipment.domain.ShipmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Compact shipment representation for listings. */
@Schema(description = "Shipment summary")
public record ShipmentSummaryResponse(
        @Schema(description = "Shipment id") UUID id,
        @Schema(description = "Order this shipment is for") UUID orderId,
        @Schema(description = "Current status", example = "IN_TRANSIT") ShipmentStatus status,
        @Schema(description = "Carrier", example = "DHL") String carrier,
        @Schema(description = "Tracking number", example = "TRK-7F3K9Q2M") String trackingNumber,
        @Schema(description = "Estimated delivery time") Instant estimatedDelivery) {}
