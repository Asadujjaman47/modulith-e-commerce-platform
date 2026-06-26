package com.company.ecommerce.shipment.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.order.spi.OrderQuery;
import com.company.ecommerce.order.spi.OrderView;
import com.company.ecommerce.shipment.api.dto.CreateShipmentRequest;
import com.company.ecommerce.shipment.domain.DeliveryAddress;
import com.company.ecommerce.shipment.domain.Shipment;
import com.company.ecommerce.shipment.domain.event.ShipmentCreatedEvent;
import com.company.ecommerce.shipment.infrastructure.mapper.ShipmentMapper;
import com.company.ecommerce.shipment.infrastructure.persistence.ShipmentRepository;
import java.math.BigDecimal;
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
class CreateShipmentUseCaseTest {

    @Mock private OrderQuery orderQuery;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private TrackingNumberGenerator trackingNumberGenerator;
    @Mock private ShipmentMapper shipmentMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private CreateShipmentUseCase useCase;

    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    private OrderView orderView(String status) {
        return new OrderView(
                orderId, "ORD-1", customerId, status, "USD", new BigDecimal("100.00"),
                "Home", "221B Baker St", null, "London", null, "NW1 6XE", "GB");
    }

    private CreateShipmentRequest request() {
        return new CreateShipmentRequest(orderId, "DHL");
    }

    @Test
    void createsShipmentForPaidOrder() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderQuery.findById(orderId)).thenReturn(Optional.of(orderView("PAID")));
        when(trackingNumberGenerator.generate()).thenReturn("TRK-TEST");
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.create(request());

        verify(shipmentRepository).save(any(Shipment.class));
        verify(eventPublisher).publishEvent(any(ShipmentCreatedEvent.class));
    }

    @Test
    void returnsExistingShipmentWhenAlreadyCreated() {
        DeliveryAddress address =
                new DeliveryAddress("Home", "l1", null, "city", null, "12345", "US");
        Shipment existing =
                Shipment.create(orderId, customerId, "DHL", "TRK-1", address, Instant.now());
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        useCase.create(request());

        verify(shipmentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rejectsUnpaidOrderForAdminCreation() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderQuery.findById(orderId)).thenReturn(Optional.of(orderView("PENDING")));

        assertThatThrownBy(() -> useCase.create(request())).isInstanceOf(BusinessException.class);
    }

    @Test
    void eventDrivenCreationSkipsPaidCheck() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderQuery.findById(orderId)).thenReturn(Optional.of(orderView("PENDING")));
        when(trackingNumberGenerator.generate()).thenReturn("TRK-TEST");
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.createForOrder(orderId);

        verify(shipmentRepository).save(any(Shipment.class));
        verify(eventPublisher).publishEvent(any(ShipmentCreatedEvent.class));
    }

    @Test
    void throwsWhenOrderNotFound() {
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderQuery.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.create(request()))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
