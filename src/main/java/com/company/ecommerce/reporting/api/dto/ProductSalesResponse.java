package com.company.ecommerce.reporting.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Unit-sales figures for a single product within a product report. Order events carry no unit price,
 * so this reports units sold and order counts rather than per-product revenue.
 */
@Schema(description = "Unit-sales figures for a single product")
public record ProductSalesResponse(
        @Schema(description = "Product id") UUID productId,
        @Schema(description = "Total units sold in the window", example = "143") long unitsSold,
        @Schema(description = "Number of distinct orders containing the product", example = "98")
                long orderCount) {}
