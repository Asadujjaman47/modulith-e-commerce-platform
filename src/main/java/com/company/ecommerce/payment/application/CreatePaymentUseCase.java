package com.company.ecommerce.payment.application;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.order.spi.OrderQuery;
import com.company.ecommerce.order.spi.OrderView;
import com.company.ecommerce.payment.api.dto.CreatePaymentRequest;
import com.company.ecommerce.payment.api.dto.PaymentResponse;
import com.company.ecommerce.payment.domain.Payment;
import com.company.ecommerce.payment.domain.PaymentStatus;
import com.company.ecommerce.payment.domain.event.PaymentCompletedEvent;
import com.company.ecommerce.payment.domain.event.PaymentFailedEvent;
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
import org.springframework.util.StringUtils;

/**
 * Processes a payment for one of the customer's orders.
 *
 * <p>Resolves the order through the {@code order} {@code spi} (validating ownership and the payable
 * amount), finds or creates the order's payment intent, then charges it through the
 * {@link PaymentGateway}. A successful charge publishes {@link PaymentCompletedEvent} (which an
 * in-module listener uses to mark the order {@code PAID}); a declined charge publishes
 * {@link PaymentFailedEvent}. The operation is idempotent: paying an already-paid order returns the
 * original payment, and a previously failed payment is retried in place.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreatePaymentUseCase {

    private final OrderQuery orderQuery;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentMapper paymentMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse process(
            UUID customerId, CreatePaymentRequest request, String idempotencyKey) {
        OrderView order =
                orderQuery
                        .findById(request.orderId())
                        .filter(view -> view.customerId().equals(customerId))
                        .orElseThrow(() -> new EntityNotFoundException("Order", request.orderId()));

        if ("CANCELLED".equals(order.status()) || "REFUNDED".equals(order.status())) {
            throw new BusinessException(
                    "Order %s cannot be paid in status %s".formatted(order.orderId(), order.status()));
        }
        if (order.totalAmount() == null || order.totalAmount().signum() <= 0) {
            throw new BusinessException("Order %s has no payable amount".formatted(order.orderId()));
        }

        Payment payment =
                paymentRepository
                        .findByOrderId(order.orderId())
                        .orElseGet(
                                () ->
                                        paymentRepository.save(
                                                Payment.createIntent(
                                                        order.orderId(),
                                                        customerId,
                                                        order.totalAmount(),
                                                        order.currency())));

        if (payment.isSuccessful()) {
            log.info("Idempotent replay of payment for order {} (already paid).", order.orderId());
            return paymentMapper.toResponse(payment);
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException(
                    "Payment for order %s was refunded and cannot be charged again"
                            .formatted(order.orderId()));
        }
        if (payment.getStatus() == PaymentStatus.FAILED) {
            payment.retry();
        }
        if (StringUtils.hasText(idempotencyKey)) {
            payment.assignIdempotencyKey(idempotencyKey);
        }

        GatewayResult result =
                paymentGateway.charge(
                        payment.getId(), payment.getAmount(), payment.getCurrency(), request.method());

        if (result.success()) {
            payment.markSucceeded(request.method(), result.reference());
            paymentRepository.save(payment);
            eventPublisher.publishEvent(
                    new PaymentCompletedEvent(
                            payment.getId(),
                            payment.getOrderId(),
                            customerId,
                            payment.getAmount(),
                            payment.getCurrency()));
            log.info(
                    "Payment succeeded. paymentId={} orderId={} amount={}",
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getAmount());
        } else {
            payment.markFailed(request.method(), result.message());
            paymentRepository.save(payment);
            eventPublisher.publishEvent(
                    new PaymentFailedEvent(
                            payment.getId(), payment.getOrderId(), customerId, result.message()));
            log.warn(
                    "Payment failed. paymentId={} orderId={} reason={}",
                    payment.getId(),
                    payment.getOrderId(),
                    result.message());
        }
        return paymentMapper.toResponse(payment);
    }
}
