package com.company.ecommerce.reporting.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Sales report for a date window: grand totals plus a per-day breakdown. */
@Schema(description = "Sales report over a date window")
public record SalesReportResponse(
        @Schema(description = "Inclusive start date", example = "2026-06-01") LocalDate from,
        @Schema(description = "Inclusive end date", example = "2026-06-30") LocalDate to,
        @Schema(description = "Total orders placed in the window", example = "240") long orderCount,
        @Schema(description = "Total units sold in the window", example = "812") long unitsSold,
        @Schema(description = "Total revenue charged (sum of order totals)", example = "29950.00")
                BigDecimal revenue,
        @Schema(description = "Total discounts applied in the window", example = "2400.00")
                BigDecimal discountTotal,
        @Schema(description = "Per-day breakdown, ordered by date") List<DailySalesResponse> daily) {}
