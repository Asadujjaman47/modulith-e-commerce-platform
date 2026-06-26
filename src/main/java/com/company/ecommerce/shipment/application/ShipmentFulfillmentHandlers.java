package com.company.ecommerce.shipment.application;

import com.company.ecommerce.order.spi.OrderLifecycle;
import com.company.ecommerce.shipment.domain.event.ShipmentCreatedEvent;
import com.company.ecommerce.shipment.domain.event.ShipmentDeliveredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Drives the order forward through its lifecycle as the shipment progresses, via the order
 * {@code spi}. Each handler runs after its shipment transaction commits, in its own transaction, so
 * the cross-module write stays within the order module's boundary (shipment → order, never the
 * reverse). The lifecycle calls are "ensure at least", so they tolerate the async ordering between
 * payment's {@code markPaid} and these handlers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentFulfillmentHandlers {

    private final OrderLifecycle orderLifecycle;

    @ApplicationModuleListener
    public void onShipmentCreated(ShipmentCreatedEvent event) {
        orderLifecycle.markProcessing(event.orderId());
        log.info("Marked order {} as processing following shipment {}.", event.orderId(), event.shipmentId());
    }

    @ApplicationModuleListener
    public void onShipmentDelivered(ShipmentDeliveredEvent event) {
        orderLifecycle.markDelivered(event.orderId());
        log.info("Marked order {} as delivered following shipment {}.", event.orderId(), event.shipmentId());
    }
}
