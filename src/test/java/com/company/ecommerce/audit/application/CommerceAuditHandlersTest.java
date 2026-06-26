package com.company.ecommerce.audit.application;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.company.ecommerce.audit.domain.AuditCategory;
import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.payment.domain.event.PaymentCompletedEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommerceAuditHandlersTest {

    @Mock private AuditLogWriter audit;
    @InjectMocks private CommerceAuditHandlers handlers;

    @Test
    void recordsOrderCreated() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        handlers.on(
                new OrderCreatedEvent(
                        orderId,
                        "ORD-1001",
                        customerId,
                        List.of(),
                        null,
                        BigDecimal.ZERO,
                        new BigDecimal("49.99")));

        verify(audit)
                .record(
                        eq(AuditCategory.ORDER),
                        eq("OrderCreated"),
                        eq("CREATE"),
                        eq("Order"),
                        eq(orderId),
                        eq(customerId),
                        contains("ORD-1001"));
    }

    @Test
    void recordsPaymentCompleted() {
        UUID paymentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        handlers.on(
                new PaymentCompletedEvent(
                        paymentId, UUID.randomUUID(), customerId, new BigDecimal("49.99"), "USD"));

        verify(audit)
                .record(
                        eq(AuditCategory.PAYMENT),
                        eq("PaymentCompleted"),
                        eq("PAYMENT"),
                        eq("Payment"),
                        eq(paymentId),
                        eq(customerId),
                        contains("succeeded"));
    }
}
