package com.company.ecommerce.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to create a brand. */
@Schema(description = "Create-brand request")
public record CreateBrandRequest(
        @Schema(description = "Display name", example = "Acme")
                @NotBlank
                @Size(max = 150)
                String name,
        @Schema(description = "URL-friendly slug; generated from the name when omitted",
                        example = "acme")
                @Size(max = 150)
                String slug,
        @Schema(description = "Description") @Size(max = 2000) String description,
        @Schema(description = "Logo URL") @Size(max = 500) String logoUrl) {}