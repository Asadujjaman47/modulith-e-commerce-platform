package com.company.ecommerce.payment.application;

import com.company.ecommerce.common.api.PageResponse;
import com.company.ecommerce.payment.api.dto.PaymentSummaryResponse;
import com.company.ecommerce.payment.domain.Payment;
import com.company.ecommerce.payment.infrastructure.mapper.PaymentMapper;
import com.company.ecommerce.payment.infrastructure.persistence.PaymentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lists the authenticated customer's payment history, optionally filtered by order. */
@Service
@RequiredArgsConstructor
public class ListPaymentsUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Transactional(readOnly = true)
    public PageResponse<PaymentSummaryResponse> listForCustomer(
            UUID customerId, UUID orderId, Pageable pageable) {
        Page<Payment> page =
                orderId == null
                        ? paymentRepository.findByCustomerId(customerId, pageable)
                        : paymentRepository.findByCustomerIdAndOrderId(customerId, orderId, pageable);
        return PageResponse.from(page.map(paymentMapper::toSummaryResponse));
    }
}
