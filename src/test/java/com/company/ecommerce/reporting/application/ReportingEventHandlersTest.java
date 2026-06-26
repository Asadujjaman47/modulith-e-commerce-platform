package com.company.ecommerce.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.order.domain.event.OrderLine;
import com.company.ecommerce.reporting.domain.ProductSalesFact;
import com.company.ecommerce.reporting.domain.SalesFact;
import com.company.ecommerce.reporting.infrastructure.persistence.ProductSalesFactRepository;
import com.company.ecommerce.reporting.infrastructure.persistence.SalesFactRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportingEventHandlersTest {

    @Mock private SalesFactRepository salesFactRepository;
    @Mock private ProductSalesFactRepository productSalesFactRepository;
    @InjectMocks private ReportingEventHandlers handlers;

    @Test
    void recordsSalesAndProductFactsForNewOrder() {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        when(salesFactRepository.existsByOrderId(orderId)).thenReturn(false);

        handlers.on(
                new OrderCreatedEvent(
                        orderId,
                        "ORD-1001",
                        UUID.randomUUID(),
                        List.of(new OrderLine(productA, 2), new OrderLine(productB, 3)),
                        null,
                        new BigDecimal("10.00"),
                        new BigDecimal("90.00")));

        ArgumentCaptor<SalesFact> salesCaptor = ArgumentCaptor.forClass(SalesFact.class);
        verify(salesFactRepository).save(salesCaptor.capture());
        assertThat(salesCaptor.getValue().getItemCount()).isEqualTo(5);
        assertThat(salesCaptor.getValue().getOrderTotal()).isEqualByComparingTo("90.00");
        assertThat(salesCaptor.getValue().getDiscountTotal()).isEqualByComparingTo("10.00");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductSalesFact>> productCaptor = ArgumentCaptor.forClass(List.class);
        verify(productSalesFactRepository).saveAll(productCaptor.capture());
        assertThat(productCaptor.getValue()).hasSize(2);
    }

    @Test
    void skipsAlreadyRecordedOrder() {
        UUID orderId = UUID.randomUUID();
        when(salesFactRepository.existsByOrderId(orderId)).thenReturn(true);

        handlers.on(
                new OrderCreatedEvent(
                        orderId,
                        "ORD-1002",
                        UUID.randomUUID(),
                        List.of(new OrderLine(UUID.randomUUID(), 1)),
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("19.99")));

        verify(salesFactRepository, never()).save(any());
        verify(productSalesFactRepository, never()).saveAll(any());
    }
}
