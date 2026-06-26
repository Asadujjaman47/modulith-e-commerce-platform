package com.company.ecommerce.reporting.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Sales figures for a single day within a sales report. */
@Schema(description = "Sales figures for a single day")
public record DailySalesResponse(
        @Schema(description = "Calendar day", example = "2026-06-26") LocalDate date,
        @Schema(description = "Number of orders placed", example = "12") long orderCount,
        @Schema(description = "Total units sold", example = "37") long unitsSold,
        @Schema(description = "Revenue charged (sum of order totals)", example = "1499.50")
                BigDecimal revenue,
        @Schema(description = "Total discounts applied", example = "120.00")
                BigDecimal discountTotal) {}
