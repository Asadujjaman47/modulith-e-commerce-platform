package com.company.ecommerce.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Request to update a product. */
@Schema(description = "Update-product request")
public record UpdateProductRequest(
        @Schema(description = "Display name", example = "UltraBook 14")
                @NotBlank
                @Size(max = 200)
                String name,
        @Schema(description = "URL-friendly slug; regenerated from the name when omitted",
                        example = "ultrabook-14")
                @Size(max = 200)
                String slug,
        @Schema(description = "Description") @Size(max = 4000) String description,
        @Schema(description = "Unit price", example = "1299.00") @NotNull @Positive BigDecimal price,
        @Schema(description = "ISO currency code", example = "USD")
                @NotBlank
                @Size(min = 3, max = 3)
                String currency,
        @Schema(description = "Owning category id") @NotNull UUID categoryId,
        @Schema(description = "Brand id, if any") UUID brandId,
        @Schema(description = "Whether the product is active", example = "true") boolean active,
        @Schema(description = "Product images (replaces the existing set)") @Valid
                List<ProductImageDto> images) {}