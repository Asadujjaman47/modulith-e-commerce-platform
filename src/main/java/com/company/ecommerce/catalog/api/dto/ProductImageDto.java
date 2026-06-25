package com.company.ecommerce.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A product image, used in both requests (input) and responses (output). */
@Schema(description = "Product image")
public record ProductImageDto(
        @Schema(description = "Image URL", example = "https://cdn.example.com/p/1.jpg")
                @NotBlank
                @Size(max = 500)
                String url,
        @Schema(description = "Alternative text", example = "Front view")
                @Size(max = 255)
                String altText,
        @Schema(description = "Whether this is the primary image", example = "true")
                boolean primary) {}