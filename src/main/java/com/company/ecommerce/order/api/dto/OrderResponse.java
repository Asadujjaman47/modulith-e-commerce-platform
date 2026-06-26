package com.company.ecommerce.order.api.dto;

import com.company.ecommerce.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full representation of an order, returned for place/details/cancel/status operations. */
@Schema(description = "Order")
public record OrderResponse(
        @Schema(description = "Order id") UUID id,
        @Schema(description = "Human-readable order number", example = "ORD-20260626-AB12CD")
                String orderNumber,
        @Schema(description = "Owning customer id") UUID customerId,
        @Schema(description = "Current status", example = "PENDING") OrderStatus status,
        @Schema(description = "Order line items") List<OrderItemResponse> items,
        @Schema(description = "Shipping address snapshot") OrderAddressResponse shippingAddress,
        @Schema(description = "Currency code", example = "USD") String currency,
        @Schema(description = "Sum of line totals", example = "2598.00") BigDecimal subtotal,
        @Schema(description = "Applied coupon code", example = "SAVE20") String couponCode,
        @Schema(description = "Discount applied", example = "200.00") BigDecimal discountAmount,
        @Schema(description = "Amount payable (subtotal − discount)", example = "2398.00")
                BigDecimal totalAmount,
        @Schema(description = "When the order was placed") Instant placedAt,
        @Schema(description = "When the order was cancelled, if applicable") Instant cancelledAt) {}
