package com.company.ecommerce.cart.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/** Request to add a product to the cart. */
@Schema(description = "Add-to-cart request")
public record AddToCartRequest(
        @Schema(description = "Catalog product id", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                @NotNull
                UUID productId,
        @Schema(description = "Quantity to add", example = "2") @NotNull @Positive Integer quantity) {}
