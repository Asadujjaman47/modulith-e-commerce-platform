package com.company.ecommerce.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.order.spi.OrderQuery;
import com.company.ecommerce.order.spi.OrderView;
import com.company.ecommerce.payment.api.dto.CreatePaymentRequest;
import com.company.ecommerce.payment.domain.Payment;
import com.company.ecommerce.payment.domain.PaymentMethod;
import com.company.ecommerce.payment.domain.PaymentStatus;
import com.company.ecommerce.payment.domain.event.PaymentCompletedEvent;
import com.company.ecommerce.payment.domain.event.PaymentFailedEvent;
import com.company.ecommerce.payment.infrastructure.gateway.PaymentGateway;
import com.company.ecommerce.payment.infrastructure.gateway.PaymentGateway.GatewayResult;
import com.company.ecommerce.payment.infrastructure.mapper.PaymentMapper;
import com.company.ecommerce.payment.infrastructure.persistence.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CreatePaymentUseCaseTest {

    @Mock private OrderQuery orderQuery;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentGateway paymentGateway;
    @Mock private PaymentMapper paymentMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private CreatePaymentUseCase useCase;

    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    private OrderView orderView(String status) {
        return new OrderView(
                orderId, "ORD-1", customerId, status, "USD", new BigDecimal("100.00"),
                "Home", "221B Baker St", null, "London", null, "NW1 6XE", "GB");
    }

    private Payment pendingIntent() {
        return Payment.createIntent(orderId, customerId, new BigDecimal("100.00"), "USD");
    }

    private CreatePaymentRequest request() {
        return new CreatePaymentRequest(orderId, PaymentMethod.CARD);
    }

    @Test
    void processesSuccessfulPayment() {
        Payment intent = pendingIntent();
        when(orderQuery.findById(orderId)).thenReturn(Optional.of(orderView("PENDING")));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(intent));
        when(paymentGateway.charge(any(), any(), anyString(), any()))
                .thenReturn(GatewayResult.approved("REF-1"));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.process(customerId, request(), "key-1");

        assertThat(intent.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(intent.getIdempotencyKey()).isEqualTo("key-1");
        verify(eventPublisher).publishEvent(any(PaymentCompletedEvent.class));
    }

    @Test
    void recordsFailedPayment() {
        Payment intent = pendingIntent();
        when(orderQuery.findById(orderId)).thenReturn(Optional.of(orderView("PENDING")));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(intent));
        when(paymentGateway.charge(any(), any(), anyString(), any()))
                .thenReturn(GatewayResult.declined("Card declined"));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.process(customerId, request(), null);

        assertThat(intent.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(eventPublisher).publishEvent(any(PaymentFailedEvent.class));
    }

    @Test
    void createsIntentWhenNoneExists() {
        when(orderQuery.findById(orderId)).thenReturn(Optional.of(orderView("PENDING")));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentGateway.charge(any(), any(), anyString(), any()))
                .thenReturn(GatewayResult.approved("REF-1"));

        useCase.process(customerId, request(), null);

        verify(paymentGateway).charge(any(), any(), anyString(), any());
        verify(eventPublisher).publishEvent(any(PaymentCompletedEvent.class));
    }

    @Test
    void isIdempotentWhenAlreadyPaid() {
        Payment paid = pendingIntent();
        paid.markSucceeded(PaymentMethod.CARD, "REF-1");
        when(orderQuery.findById(orderId)).thenReturn(Optional.of(orderView("PAID")));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(paid));

        useCase.process(customerId, request(), null);

        verify(paymentGateway, never()).charge(any(), any(), anyString(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsForeignOrder() {
        OrderView foreign =
                new OrderView(
                        orderId, "ORD-1", UUID.randomUUID(), "PENDING", "USD",
                        new BigDecimal("100.00"), null, "x", null, "y", null, "z", "US");
        when(orderQuery.findById(orderId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> useCase.process(customerId, request(), null))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void rejectsCancelledOrder() {
        when(orderQuery.findById(orderId)).thenReturn(Optional.of(orderView("CANCELLED")));

        assertThatThrownBy(() -> useCase.process(customerId, request(), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void throwsWhenOrderNotFound() {
        when(orderQuery.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.process(customerId, request(), null))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
