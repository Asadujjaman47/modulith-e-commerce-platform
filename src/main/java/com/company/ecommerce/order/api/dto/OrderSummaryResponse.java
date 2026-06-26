package com.company.ecommerce.order.api.dto;

import com.company.ecommerce.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Condensed order representation for history/list endpoints (no line items). */
@Schema(description = "Order summary")
public record OrderSummaryResponse(
        @Schema(description = "Order id") UUID id,
        @Schema(description = "Human-readable order number", example = "ORD-20260626-AB12CD")
                String orderNumber,
        @Schema(description = "Current status", example = "PENDING") OrderStatus status,
        @Schema(description = "Number of line items", example = "3") int itemCount,
        @Schema(description = "Amount payable", example = "2398.00") BigDecimal totalAmount,
        @Schema(description = "When the order was placed") Instant placedAt) {}
