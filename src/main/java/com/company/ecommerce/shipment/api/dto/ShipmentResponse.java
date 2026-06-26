package com.company.ecommerce.shipment.api.dto;

import com.company.ecommerce.shipment.domain.ShipmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full representation of a shipment, including its tracking history. */
@Schema(description = "Shipment")
public record ShipmentResponse(
        @Schema(description = "Shipment id") UUID id,
        @Schema(description = "Order this shipment is for") UUID orderId,
        @Schema(description = "Owning customer id") UUID customerId,
        @Schema(description = "Current status", example = "IN_TRANSIT") ShipmentStatus status,
        @Schema(description = "Carrier", example = "DHL") String carrier,
        @Schema(description = "Tracking number", example = "TRK-7F3K9Q2M") String trackingNumber,
        @Schema(description = "Delivery address snapshot") DeliveryAddressResponse deliveryAddress,
        @Schema(description = "When the shipment was dispatched") Instant shippedAt,
        @Schema(description = "When the shipment was delivered") Instant deliveredAt,
        @Schema(description = "Estimated delivery time") Instant estimatedDelivery,
        @Schema(description = "Tracking history, oldest first") List<TrackingRecordResponse> trackingRecords) {

    /** Snapshotted delivery address. */
    @Schema(description = "Delivery address")
    public record DeliveryAddressResponse(
            @Schema(description = "Label", example = "Home") String label,
            @Schema(description = "Address line 1", example = "221B Baker St") String line1,
            @Schema(description = "Address line 2") String line2,
            @Schema(description = "City", example = "London") String city,
            @Schema(description = "State/region") String state,
            @Schema(description = "Postal code", example = "NW1 6XE") String postalCode,
            @Schema(description = "Country", example = "GB") String country) {}
}
