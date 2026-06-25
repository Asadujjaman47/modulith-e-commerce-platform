package com.company.ecommerce.cart.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** A customer's shopping cart. */
@Schema(description = "Shopping cart")
public record CartResponse(
        @Schema(description = "Cart id") UUID id,
        @Schema(description = "Owning customer id") UUID customerId,
        @Schema(description = "Cart line items") List<CartItemResponse> items,
        @Schema(description = "Sum of all line totals", example = "2598.00") BigDecimal subtotal) {}
