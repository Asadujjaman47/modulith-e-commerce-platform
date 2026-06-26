package com.company.ecommerce.shipment.application;

import static org.mockito.Mockito.verify;

import com.company.ecommerce.order.spi.OrderLifecycle;
import com.company.ecommerce.shipment.domain.event.ShipmentCreatedEvent;
import com.company.ecommerce.shipment.domain.event.ShipmentDeliveredEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipmentFulfillmentHandlersTest {

    @Mock private OrderLifecycle orderLifecycle;
    @InjectMocks private ShipmentFulfillmentHandlers handlers;

    @Test
    void marksOrderProcessingOnShipmentCreated() {
        UUID orderId = UUID.randomUUID();
        handlers.onShipmentCreated(
                new ShipmentCreatedEvent(UUID.randomUUID(), orderId, UUID.randomUUID(), "TRK-1", "DHL"));

        verify(orderLifecycle).markProcessing(orderId);
    }

    @Test
    void marksOrderDeliveredOnShipmentDelivered() {
        UUID orderId = UUID.randomUUID();
        handlers.onShipmentDelivered(
                new ShipmentDeliveredEvent(UUID.randomUUID(), orderId, UUID.randomUUID(), Instant.now()));

        verify(orderLifecycle).markDelivered(orderId);
    }
}
