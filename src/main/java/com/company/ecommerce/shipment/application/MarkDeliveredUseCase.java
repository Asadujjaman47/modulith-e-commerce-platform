package com.company.ecommerce.shipment.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.shipment.api.dto.ShipmentResponse;
import com.company.ecommerce.shipment.domain.Shipment;
import com.company.ecommerce.shipment.domain.event.ShipmentDeliveredEvent;
import com.company.ecommerce.shipment.infrastructure.mapper.ShipmentMapper;
import com.company.ecommerce.shipment.infrastructure.persistence.ShipmentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirms delivery of a shipment and publishes {@link ShipmentDeliveredEvent} (an in-module listener
 * then marks the order {@code DELIVERED} via the order {@code spi}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarkDeliveredUseCase {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentMapper shipmentMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ShipmentResponse deliver(UUID shipmentId) {
        return deliver(shipmentId, null, "Delivered");
    }

    @Transactional
    public ShipmentResponse deliver(UUID shipmentId, String location, String note) {
        Shipment shipment =
                shipmentRepository
                        .findById(shipmentId)
                        .orElseThrow(() -> new EntityNotFoundException("Shipment", shipmentId));

        shipment.markDelivered(location, note == null ? "Delivered" : note);
        eventPublisher.publishEvent(
                new ShipmentDeliveredEvent(
                        shipment.getId(),
                        shipment.getOrderId(),
                        shipment.getCustomerId(),
                        shipment.getDeliveredAt()));
        log.info("Shipment delivered. shipmentId={} orderId={}", shipmentId, shipment.getOrderId());
        return shipmentMapper.toResponse(shipment);
    }
}
