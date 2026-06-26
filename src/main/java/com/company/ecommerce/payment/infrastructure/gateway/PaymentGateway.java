package com.company.ecommerce.payment.infrastructure.gateway;

import com.company.ecommerce.payment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Abstraction over an external payment provider. Kept behind an interface so a real provider (Stripe,
 * Adyen, …) can replace the {@link SimulatedPaymentGateway} without touching the payment use cases.
 */
public interface PaymentGateway {

    /** Attempts to charge the given amount for a payment. */
    GatewayResult charge(UUID paymentId, BigDecimal amount, String currency, PaymentMethod method);

    /** Refunds a previously successful charge identified by its gateway reference. */
    GatewayResult refund(String gatewayReference, BigDecimal amount);

    /** Outcome of a gateway call: success flag, the provider reference, and a human-readable message. */
    record GatewayResult(boolean success, String reference, String message) {

        public static GatewayResult approved(String reference) {
            return new GatewayResult(true, reference, "Approved");
        }

        public static GatewayResult declined(String message) {
            return new GatewayResult(false, null, message);
        }
    }
}
