package com.company.ecommerce.shipment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.shipment.domain.DeliveryAddress;
import com.company.ecommerce.shipment.domain.Shipment;
import com.company.ecommerce.shipment.domain.ShipmentStatus;
import com.company.ecommerce.shipment.domain.event.ShipmentDeliveredEvent;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MarkDeliveredUseCaseTest {

    @Mock private ShipmentRepository shipmentRepository;
    @Mock private ShipmentMapper shipmentMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private MarkDeliveredUseCase useCase;

    private final UUID shipmentId = UUID.randomUUID();

    private Shipment shipment() {
        DeliveryAddress address =
                new DeliveryAddress("Home", "l1", null, "city", null, "12345", "US");
        return Shipment.create(
                UUID.randomUUID(), UUID.randomUUID(), "DHL", "TRK-1", address, Instant.now());
    }

    @Test
    void deliversAndPublishesEvent() {
        Shipment shipment = shipment();
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        useCase.deliver(shipmentId);

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        verify(eventPublisher).publishEvent(any(ShipmentDeliveredEvent.class));
    }

    @Test
    void rejectsAlreadyDelivered() {
        Shipment shipment = shipment();
        shipment.markDelivered(null, null);
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        assertThatThrownBy(() -> useCase.deliver(shipmentId)).isInstanceOf(BusinessException.class);
    }

    @Test
    void throwsWhenShipmentNotFound() {
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.deliver(shipmentId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
