package com.company.ecommerce.reporting.application;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Aggregated sales figures for a single day (JPQL projection). */
public record DailySalesRow(
        LocalDate date,
        long orderCount,
        long unitsSold,
        BigDecimal grossRevenue,
        BigDecimal discountTotal) {}
