package com.company.ecommerce.order.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** The shipping address snapshot of an order. */
@Schema(description = "Order shipping address")
public record OrderAddressResponse(
        @Schema(description = "Label", example = "Home") String label,
        @Schema(description = "Address line 1", example = "221B Baker Street") String line1,
        @Schema(description = "Address line 2", example = "Apt 4") String line2,
        @Schema(description = "City", example = "London") String city,
        @Schema(description = "State / region", example = "Greater London") String state,
        @Schema(description = "Postal code", example = "NW1 6XE") String postalCode,
        @Schema(description = "Country", example = "GB") String country) {}
