package com.company.ecommerce.shipment.application;

import com.company.ecommerce.payment.domain.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Creates a shipment for an order once its payment completes. Runs after the payment transaction
 * commits, in its own transaction. Consumes the {@code payment} module's {@code events} named
 * interface (shipment → payment); the shipment, in turn, drives the order forward via the order
 * {@code spi}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentCreationHandler {

    private final CreateShipmentUseCase createShipmentUseCase;

    @ApplicationModuleListener
    public void on(PaymentCompletedEvent event) {
        createShipmentUseCase.createForOrder(event.orderId());
        log.info("Initiated shipment for paid order {}.", event.orderId());
    }
}
