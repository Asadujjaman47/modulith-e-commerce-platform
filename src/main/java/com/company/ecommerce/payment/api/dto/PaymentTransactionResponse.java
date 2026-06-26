package com.company.ecommerce.payment.api.dto;

import com.company.ecommerce.payment.domain.PaymentTransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A single entry in a payment's gateway-interaction ledger. */
@Schema(description = "Payment transaction ledger entry")
public record PaymentTransactionResponse(
        @Schema(description = "Transaction id") UUID id,
        @Schema(description = "Interaction type", example = "CHARGE") PaymentTransactionType type,
        @Schema(description = "Whether the gateway approved the interaction", example = "true")
                boolean succeeded,
        @Schema(description = "Amount", example = "2398.00") BigDecimal amount,
        @Schema(description = "Provider reference", example = "SIM-CHG-1a2b") String gatewayReference,
        @Schema(description = "Human-readable message", example = "Charge approved") String message,
        @Schema(description = "When the interaction was recorded") Instant occurredAt) {}
