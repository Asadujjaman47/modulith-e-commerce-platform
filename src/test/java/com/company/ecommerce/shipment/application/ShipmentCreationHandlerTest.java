package com.company.ecommerce.shipment.application;

import static org.mockito.Mockito.verify;

import com.company.ecommerce.payment.domain.event.PaymentCompletedEvent;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipmentCreationHandlerTest {

    @Mock private CreateShipmentUseCase createShipmentUseCase;
    @InjectMocks private ShipmentCreationHandler handler;

    @Test
    void createsShipmentOnPaymentCompleted() {
        UUID orderId = UUID.randomUUID();
        handler.on(
                new PaymentCompletedEvent(
                        UUID.randomUUID(), orderId, UUID.randomUUID(), new BigDecimal("100.00"), "USD"));

        verify(createShipmentUseCase).createForOrder(orderId);
    }
}
