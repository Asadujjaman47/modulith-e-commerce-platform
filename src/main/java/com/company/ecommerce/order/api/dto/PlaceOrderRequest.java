package com.company.ecommerce.order.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Request to place an order from the authenticated customer's current cart.
 *
 * <p>The order contents come from the cart; the caller only chooses the shipping address and may
 * optionally supply a coupon code.
 */
@Schema(description = "Place-order request")
public record PlaceOrderRequest(
        @Schema(
                        description = "Id of one of the customer's saved addresses to ship to",
                        example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                @NotNull
                UUID addressId,
        @Schema(description = "Optional coupon code to apply", example = "SAVE20")
                @Size(max = 64)
                String couponCode) {}
