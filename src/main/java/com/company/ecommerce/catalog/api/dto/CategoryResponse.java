package com.company.ecommerce.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Category representation returned to clients. */
@Schema(description = "Product category")
public record CategoryResponse(
        @Schema(description = "Category id") UUID id,
        @Schema(description = "Display name", example = "Laptops") String name,
        @Schema(description = "URL-friendly slug", example = "laptops") String slug,
        @Schema(description = "Description") String description,
        @Schema(description = "Parent category id, if this is a sub-category") UUID parentCategoryId,
        @Schema(description = "Whether the category is active", example = "true") boolean active) {}