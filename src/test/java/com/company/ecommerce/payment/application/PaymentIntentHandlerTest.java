package com.company.ecommerce.payment.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.order.spi.OrderQuery;
import com.company.ecommerce.order.spi.OrderView;
import com.company.ecommerce.payment.domain.Payment;
import com.company.ecommerce.payment.infrastructure.persistence.PaymentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentIntentHandlerTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderQuery orderQuery;
    @InjectMocks private PaymentIntentHandler handler;

    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    private OrderCreatedEvent event(BigDecimal total) {
        return new OrderCreatedEvent(
                orderId, "ORD-1", customerId, List.of(), null, BigDecimal.ZERO, total);
    }

    @Test
    void createsPendingIntent() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderQuery.findById(orderId))
                .thenReturn(
                        Optional.of(
                                new OrderView(
                                        orderId, "ORD-1", customerId, "PENDING", "USD",
                                        new BigDecimal("100.00"), null, "x", null, "y", null, "z", "US")));

        handler.on(event(new BigDecimal("100.00")));

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void skipsWhenAlreadyExists() {
        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(
                        Optional.of(
                                Payment.createIntent(
                                        orderId, customerId, new BigDecimal("100.00"), "USD")));

        handler.on(event(new BigDecimal("100.00")));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void skipsZeroTotalOrders() {
        handler.on(event(BigDecimal.ZERO));

        verify(paymentRepository, never()).save(any());
    }
}
