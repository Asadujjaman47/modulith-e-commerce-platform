package com.company.ecommerce.shipment.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.shipment.api.dto.ShipmentResponse;
import com.company.ecommerce.shipment.api.dto.UpdateShipmentStatusRequest;
import com.company.ecommerce.shipment.domain.Shipment;
import com.company.ecommerce.shipment.infrastructure.mapper.ShipmentMapper;
import com.company.ecommerce.shipment.infrastructure.persistence.ShipmentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-driven shipment status progression, guarded by the {@code ShipmentStatus} state machine. Each
 * change appends a tracking record. Delivery is handled by {@link MarkDeliveredUseCase} so the
 * {@code ShipmentDeliveredEvent} is published consistently.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateShipmentStatusUseCase {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentMapper shipmentMapper;
    private final MarkDeliveredUseCase markDeliveredUseCase;

    @Transactional
    public ShipmentResponse updateStatus(UUID shipmentId, UpdateShipmentStatusRequest request) {
        if (request.status().isDelivered()) {
            return markDeliveredUseCase.deliver(shipmentId, request.location(), request.note());
        }

        Shipment shipment =
                shipmentRepository
                        .findById(shipmentId)
                        .orElseThrow(() -> new EntityNotFoundException("Shipment", shipmentId));

        shipment.advanceTo(request.status(), request.location(), request.note());
        log.info("Shipment status updated. shipmentId={} status={}", shipmentId, request.status());
        return shipmentMapper.toResponse(shipment);
    }
}
