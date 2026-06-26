package com.company.ecommerce.payment.application;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.payment.api.dto.PaymentResponse;
import com.company.ecommerce.payment.domain.Payment;
import com.company.ecommerce.payment.domain.PaymentStatus;
import com.company.ecommerce.payment.domain.event.PaymentRefundedEvent;
import com.company.ecommerce.payment.infrastructure.gateway.PaymentGateway;
import com.company.ecommerce.payment.infrastructure.gateway.PaymentGateway.GatewayResult;
import com.company.ecommerce.payment.infrastructure.mapper.PaymentMapper;
import com.company.ecommerce.payment.infrastructure.persistence.PaymentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refunds a successful payment (admin). Idempotent: refunding an already-refunded payment returns it
 * unchanged. Publishes {@link PaymentRefundedEvent} on success.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentMapper paymentMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse refund(UUID paymentId) {
        Payment payment =
                paymentRepository
                        .findById(paymentId)
                        .orElseThrow(() -> new EntityNotFoundException("Payment", paymentId));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("Idempotent replay of refund for payment {} (already refunded).", paymentId);
            return paymentMapper.toResponse(payment);
        }
        if (!payment.isSuccessful()) {
            throw new BusinessException(
                    "Only a successful payment can be refunded (payment is %s)".formatted(payment.getStatus()));
        }

        GatewayResult result =
                paymentGateway.refund(payment.getGatewayReference(), payment.getAmount());
        if (!result.success()) {
            throw new BusinessException("Refund declined: " + result.message());
        }

        payment.refund(result.reference());
        paymentRepository.save(payment);
        eventPublisher.publishEvent(
                new PaymentRefundedEvent(
                        payment.getId(),
                        payment.getOrderId(),
                        payment.getCustomerId(),
                        payment.getAmount()));
        log.info(
                "Payment refunded. paymentId={} orderId={} amount={}",
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount());
        return paymentMapper.toResponse(payment);
    }
}
