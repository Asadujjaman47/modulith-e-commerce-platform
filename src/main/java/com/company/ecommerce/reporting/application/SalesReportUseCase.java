package com.company.ecommerce.reporting.application;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.reporting.api.dto.DailySalesResponse;
import com.company.ecommerce.reporting.api.dto.SalesReportResponse;
import com.company.ecommerce.reporting.infrastructure.persistence.SalesFactRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds a sales report (grand totals + per-day breakdown) from the recorded sales facts. */
@Service
@RequiredArgsConstructor
public class SalesReportUseCase {

    private final SalesFactRepository salesFactRepository;

    @Transactional(readOnly = true)
    public SalesReportResponse report(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("Report start date must not be after the end date");
        }

        SalesTotalsRow totals = salesFactRepository.totals(from, to);
        List<DailySalesResponse> daily =
                salesFactRepository.dailyBreakdown(from, to).stream()
                        .map(
                                row ->
                                        new DailySalesResponse(
                                                row.date(),
                                                row.orderCount(),
                                                row.unitsSold(),
                                                scale(row.grossRevenue()),
                                                scale(row.discountTotal())))
                        .toList();

        return new SalesReportResponse(
                from,
                to,
                totals.orderCount(),
                totals.unitsSold(),
                scale(totals.grossRevenue()),
                scale(totals.discountTotal()),
                daily);
    }

    private static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
