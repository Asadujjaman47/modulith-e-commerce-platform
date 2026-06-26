package com.company.ecommerce.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.api.PageResponse;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.reporting.api.dto.ProductSalesResponse;
import com.company.ecommerce.reporting.infrastructure.persistence.ProductSalesFactRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductReportUseCaseTest {

    @Mock private ProductSalesFactRepository productSalesFactRepository;
    @InjectMocks private ProductReportUseCase useCase;

    @Test
    void buildsTopProductsPage() {
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        UUID productId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        when(productSalesFactRepository.topProducts(eq(from), eq(to), eq(pageable)))
                .thenReturn(
                        new PageImpl<>(
                                List.of(new ProductSalesRow(productId, 143, 98)), pageable, 1));

        PageResponse<ProductSalesResponse> page = useCase.report(from, to, pageable);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content().get(0).productId()).isEqualTo(productId);
        assertThat(page.content().get(0).unitsSold()).isEqualTo(143);
        assertThat(page.content().get(0).orderCount()).isEqualTo(98);
    }

    @Test
    void rejectsInvertedDateWindow() {
        assertThatThrownBy(
                        () ->
                                useCase.report(
                                        LocalDate.of(2026, 6, 30),
                                        LocalDate.of(2026, 6, 1),
                                        PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class);
    }
}
