package com.company.ecommerce.payment.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.payment.api.dto.PaymentResponse;
import com.company.ecommerce.payment.infrastructure.mapper.PaymentMapper;
import com.company.ecommerce.payment.infrastructure.persistence.PaymentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads a single payment, either scoped to its owning customer or unrestricted for admins. */
@Service
@RequiredArgsConstructor
public class GetPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    /** Returns the payment if it belongs to {@code customerId}, otherwise 404. */
    @Transactional(readOnly = true)
    public PaymentResponse getForCustomer(UUID customerId, UUID paymentId) {
        return paymentRepository
                .findByIdAndCustomerId(paymentId, customerId)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Payment", paymentId));
    }

    /** Returns any payment by id (admin). */
    @Transactional(readOnly = true)
    public PaymentResponse getById(UUID paymentId) {
        return paymentRepository
                .findById(paymentId)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Payment", paymentId));
    }
}
