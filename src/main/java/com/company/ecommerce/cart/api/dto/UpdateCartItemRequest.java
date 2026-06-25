package com.company.ecommerce.cart.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Request to set the quantity of an existing cart item. */
@Schema(description = "Update-cart-item request")
public record UpdateCartItemRequest(
        @Schema(description = "New absolute quantity", example = "3") @NotNull @Positive
                Integer quantity) {}
