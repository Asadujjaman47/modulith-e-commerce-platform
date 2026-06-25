package com.company.ecommerce.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Product representation returned to clients. */
@Schema(description = "Catalog product")
public record ProductResponse(
        @Schema(description = "Product id") UUID id,
        @Schema(description = "Display name", example = "UltraBook 14") String name,
        @Schema(description = "URL-friendly slug", example = "ultrabook-14") String slug,
        @Schema(description = "Stock keeping unit", example = "UB-14-2026") String sku,
        @Schema(description = "Description") String description,
        @Schema(description = "Unit price", example = "1299.00") BigDecimal price,
        @Schema(description = "ISO currency code", example = "USD") String currency,
        @Schema(description = "Owning category id") UUID categoryId,
        @Schema(description = "Brand id, if any") UUID brandId,
        @Schema(description = "Whether the product is active", example = "true") boolean active,
        @Schema(description = "Product images") List<ProductImageDto> images) {}