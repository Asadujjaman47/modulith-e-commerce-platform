package com.company.ecommerce.reporting.application;

import java.math.BigDecimal;

/** Aggregated grand totals across a sales reporting window (JPQL projection). */
public record SalesTotalsRow(
        long orderCount, long unitsSold, BigDecimal grossRevenue, BigDecimal discountTotal) {}
