package com.company.ecommerce.order.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

/** A single line in an order. */
@Schema(description = "Order line item")
public record OrderItemResponse(
        @Schema(description = "Order item id") UUID id,
        @Schema(description = "Catalog product id") UUID productId,
        @Schema(description = "Product name snapshot", example = "UltraBook 14") String productName,
        @Schema(description = "Unit price snapshot", example = "1299.00") BigDecimal unitPrice,
        @Schema(description = "Quantity", example = "2") int quantity,
        @Schema(description = "Line total (unit price × quantity)", example = "2598.00")
                BigDecimal lineTotal) {}
