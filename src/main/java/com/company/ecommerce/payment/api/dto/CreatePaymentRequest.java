package com.company.ecommerce.payment.api.dto;

import com.company.ecommerce.payment.domain.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request to process payment for one of the authenticated customer's orders. */
@Schema(description = "Process-payment request")
public record CreatePaymentRequest(
        @Schema(
                        description = "Id of the order to pay for",
                        example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
                @NotNull
                UUID orderId,
        @Schema(description = "Chosen payment method", example = "CARD") @NotNull
                PaymentMethod method) {}
