package com.company.ecommerce.payment.infrastructure.mapper;

import com.company.ecommerce.payment.api.dto.PaymentResponse;
import com.company.ecommerce.payment.api.dto.PaymentSummaryResponse;
import com.company.ecommerce.payment.api.dto.PaymentTransactionResponse;
import com.company.ecommerce.payment.domain.Payment;
import com.company.ecommerce.payment.domain.PaymentTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps payment aggregates to response DTOs. */
@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toResponse(Payment payment);

    PaymentSummaryResponse toSummaryResponse(Payment payment);

    @Mapping(target = "occurredAt", source = "createdAt")
    PaymentTransactionResponse toTransactionResponse(PaymentTransaction transaction);
}
