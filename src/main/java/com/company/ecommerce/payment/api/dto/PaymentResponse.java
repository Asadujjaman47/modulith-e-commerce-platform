package com.company.ecommerce.payment.api.dto;

import com.company.ecommerce.payment.domain.PaymentMethod;
import com.company.ecommerce.payment.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full representation of a payment, returned for process/details/refund operations. */
@Schema(description = "Payment")
public record PaymentResponse(
        @Schema(description = "Payment id") UUID id,
        @Schema(description = "Order this payment is for") UUID orderId,
        @Schema(description = "Owning customer id") UUID customerId,
        @Schema(description = "Current status", example = "SUCCESS") PaymentStatus status,
        @Schema(description = "Payment method", example = "CARD") PaymentMethod method,
        @Schema(description = "Amount charged", example = "2398.00") BigDecimal amount,
        @Schema(description = "Currency code", example = "USD") String currency,
        @Schema(description = "Provider reference", example = "SIM-CHG-1a2b") String gatewayReference,
        @Schema(description = "Failure reason, if the charge failed") String failureReason,
        @Schema(description = "When the payment succeeded") Instant paidAt,
        @Schema(description = "When the payment failed") Instant failedAt,
        @Schema(description = "When the payment was refunded") Instant refundedAt,
        @Schema(description = "Gateway-interaction ledger") List<PaymentTransactionResponse> transactions) {}
