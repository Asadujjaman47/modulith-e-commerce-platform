package com.company.ecommerce.payment.infrastructure.gateway;

import com.company.ecommerce.payment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Default in-process {@link PaymentGateway} used until a real provider is integrated. It approves
 * every well-formed charge and refund, returning a synthetic provider reference. The approval logic
 * lives behind the {@link PaymentGateway} interface so swapping in a real provider requires no change
 * to the payment use cases.
 */
@Slf4j
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public GatewayResult charge(
            UUID paymentId, BigDecimal amount, String currency, PaymentMethod method) {
        String reference = "SIM-CHG-" + UUID.randomUUID();
        log.info(
                "Simulated gateway approved charge. paymentId={} amount={} {} method={} reference={}",
                paymentId,
                amount,
                currency,
                method,
                reference);
        return GatewayResult.approved(reference);
    }

    @Override
    public GatewayResult refund(String gatewayReference, BigDecimal amount) {
        String reference = "SIM-RFD-" + UUID.randomUUID();
        log.info(
                "Simulated gateway issued refund. charge={} amount={} reference={}",
                gatewayReference,
                amount,
                reference);
        return GatewayResult.approved(reference);
    }
}
