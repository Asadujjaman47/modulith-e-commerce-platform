package com.company.ecommerce.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Request to update a category. */
@Schema(description = "Update-category request")
public record UpdateCategoryRequest(
        @Schema(description = "Display name", example = "Laptops")
                @NotBlank
                @Size(max = 150)
                String name,
        @Schema(description = "URL-friendly slug; regenerated from the name when omitted",
                        example = "laptops")
                @Size(max = 150)
                String slug,
        @Schema(description = "Description") @Size(max = 2000) String description,
        @Schema(description = "Parent category id, for sub-categories") UUID parentCategoryId,
        @Schema(description = "Whether the category is active", example = "true") boolean active) {}