package com.company.ecommerce.shipment.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.shipment.api.dto.ShipmentResponse;
import com.company.ecommerce.shipment.infrastructure.mapper.ShipmentMapper;
import com.company.ecommerce.shipment.infrastructure.persistence.ShipmentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads a shipment with its tracking history, scoped to its owner or unrestricted for admins. */
@Service
@RequiredArgsConstructor
public class TrackShipmentUseCase {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentMapper shipmentMapper;

    /** Returns the shipment if it belongs to {@code customerId}, otherwise 404. */
    @Transactional(readOnly = true)
    public ShipmentResponse trackForCustomer(UUID customerId, UUID shipmentId) {
        return shipmentRepository
                .findByIdAndCustomerId(shipmentId, customerId)
                .map(shipmentMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Shipment", shipmentId));
    }

    /** Returns any shipment by id (admin). */
    @Transactional(readOnly = true)
    public ShipmentResponse getById(UUID shipmentId) {
        return shipmentRepository
                .findById(shipmentId)
                .map(shipmentMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Shipment", shipmentId));
    }
}
