package com.company.ecommerce.shipment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.shipment.api.dto.UpdateShipmentStatusRequest;
import com.company.ecommerce.shipment.domain.DeliveryAddress;
import com.company.ecommerce.shipment.domain.Shipment;
import com.company.ecommerce.shipment.domain.ShipmentStatus;
import com.company.ecommerce.shipment.infrastructure.mapper.ShipmentMapper;
import com.company.ecommerce.shipment.infrastructure.persistence.ShipmentRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateShipmentStatusUseCaseTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private ShipmentMapper shipmentMapper;
    @Mock private MarkDeliveredUseCase markDeliveredUseCase;
    @InjectMocks private UpdateShipmentStatusUseCase useCase;

    private final UUID shipmentId = UUID.randomUUID();

    private Shipment pickedUpShipment() {
        DeliveryAddress address =
                new DeliveryAddress("Home", "l1", null, "city", null, "12345", "US");
        Shipment shipment =
                Shipment.create(
                        UUID.randomUUID(), UUID.randomUUID(), "DHL", "TRK-1", address, Instant.now());
        shipment.advanceTo(ShipmentStatus.PICKED_UP, null, null);
        return shipment;
    }

    @Test
    void advancesStatusStepByStep() {
        Shipment shipment = pickedUpShipment();
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        useCase.updateStatus(
                shipmentId,
                new UpdateShipmentStatusRequest(ShipmentStatus.IN_TRANSIT, "Hub", "Moving"));

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
    }

    @Test
    void delegatesDeliveredToMarkDeliveredUseCase() {
        useCase.updateStatus(
                shipmentId,
                new UpdateShipmentStatusRequest(ShipmentStatus.DELIVERED, "Door", "Done"));

        verify(markDeliveredUseCase).deliver(eq(shipmentId), any(), any());
    }
}
