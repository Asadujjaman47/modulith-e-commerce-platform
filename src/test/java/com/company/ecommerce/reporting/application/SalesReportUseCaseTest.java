package com.company.ecommerce.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.reporting.api.dto.SalesReportResponse;
import com.company.ecommerce.reporting.infrastructure.persistence.SalesFactRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesReportUseCaseTest {

    @Mock private SalesFactRepository salesFactRepository;
    @InjectMocks private SalesReportUseCase useCase;

    @Test
    void buildsReportWithTotalsAndDailyBreakdown() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 2);
        when(salesFactRepository.totals(from, to))
                .thenReturn(new SalesTotalsRow(3, 7, new BigDecimal("150.5"), new BigDecimal("20")));
        when(salesFactRepository.dailyBreakdown(from, to))
                .thenReturn(
                        List.of(
                                new DailySalesRow(
                                        from, 1, 2, new BigDecimal("50.5"), new BigDecimal("5")),
                                new DailySalesRow(
                                        to, 2, 5, new BigDecimal("100"), new BigDecimal("15"))));

        SalesReportResponse report = useCase.report(from, to);

        assertThat(report.orderCount()).isEqualTo(3);
        assertThat(report.unitsSold()).isEqualTo(7);
        assertThat(report.revenue()).isEqualByComparingTo("150.50");
        assertThat(report.discountTotal()).isEqualByComparingTo("20.00");
        assertThat(report.daily()).hasSize(2);
        assertThat(report.daily().get(0).revenue()).isEqualByComparingTo("50.50");
    }

    @Test
    void rejectsInvertedDateWindow() {
        assertThatThrownBy(
                        () -> useCase.report(LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 1)))
                .isInstanceOf(BusinessException.class);
    }
}
