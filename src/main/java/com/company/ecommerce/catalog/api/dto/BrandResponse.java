package com.company.ecommerce.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Brand representation returned to clients. */
@Schema(description = "Product brand")
public record BrandResponse(
        @Schema(description = "Brand id") UUID id,
        @Schema(description = "Display name", example = "Acme") String name,
        @Schema(description = "URL-friendly slug", example = "acme") String slug,
        @Schema(description = "Description") String description,
        @Schema(description = "Logo URL") String logoUrl,
        @Schema(description = "Whether the brand is active", example = "true") boolean active) {}