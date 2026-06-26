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
import com.company.ecommerce.payment.domain.Payment;
import com.company.ecommerce.payment.domain.PaymentMethod;
import com.company.ecommerce.payment.domain.PaymentStatus;
import com.company.ecommerce.payment.domain.event.PaymentRefundedEvent;
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
class RefundPaymentUseCaseTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentGateway paymentGateway;
    @Mock private PaymentMapper paymentMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private RefundPaymentUseCase useCase;

    private final UUID paymentId = UUID.randomUUID();

    private Payment successfulPayment() {
        Payment payment =
                Payment.createIntent(
                        UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), "USD");
        payment.markSucceeded(PaymentMethod.CARD, "REF-1");
        return payment;
    }

    @Test
    void refundsSuccessfulPayment() {
        Payment payment = successfulPayment();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentGateway.refund(anyString(), any())).thenReturn(GatewayResult.approved("RFD-1"));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.refund(paymentId);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(eventPublisher).publishEvent(any(PaymentRefundedEvent.class));
    }

    @Test
    void isIdempotentWhenAlreadyRefunded() {
        Payment payment = successfulPayment();
        payment.refund("RFD-0");
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        useCase.refund(paymentId);

        verify(paymentGateway, never()).refund(anyString(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsNonSuccessfulPayment() {
        Payment pending =
                Payment.createIntent(
                        UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), "USD");
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> useCase.refund(paymentId)).isInstanceOf(BusinessException.class);
    }

    @Test
    void throwsWhenPaymentNotFound() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.refund(paymentId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
