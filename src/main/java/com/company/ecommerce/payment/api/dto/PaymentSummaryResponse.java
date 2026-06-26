package com.company.ecommerce.payment.api.dto;

import com.company.ecommerce.payment.domain.PaymentMethod;
import com.company.ecommerce.payment.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Compact payment representation for history listings. */
@Schema(description = "Payment summary")
public record PaymentSummaryResponse(
        @Schema(description = "Payment id") UUID id,
        @Schema(description = "Order this payment is for") UUID orderId,
        @Schema(description = "Current status", example = "SUCCESS") PaymentStatus status,
        @Schema(description = "Payment method", example = "CARD") PaymentMethod method,
        @Schema(description = "Amount", example = "2398.00") BigDecimal amount,
        @Schema(description = "Currency code", example = "USD") String currency,
        @Schema(description = "When the payment succeeded") Instant paidAt) {}
