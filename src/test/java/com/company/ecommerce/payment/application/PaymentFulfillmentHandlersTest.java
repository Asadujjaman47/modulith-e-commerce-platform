package com.company.ecommerce.payment.application;

import static org.mockito.Mockito.verify;

import com.company.ecommerce.order.spi.OrderLifecycle;
import com.company.ecommerce.payment.domain.event.PaymentCompletedEvent;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentFulfillmentHandlersTest {

    @Mock private OrderLifecycle orderLifecycle;
    @InjectMocks private PaymentFulfillmentHandlers handlers;

    @Test
    void marksOrderPaidOnPaymentCompleted() {
        UUID orderId = UUID.randomUUID();
        handlers.markOrderPaid(
                new PaymentCompletedEvent(
                        UUID.randomUUID(), orderId, UUID.randomUUID(), new BigDecimal("100.00"), "USD"));

        verify(orderLifecycle).markPaid(orderId);
    }
}
