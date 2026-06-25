package com.company.ecommerce.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.inventory.api.dto.ReleaseStockRequest;
import com.company.ecommerce.inventory.domain.Inventory;
import com.company.ecommerce.inventory.domain.InventoryTransaction;
import com.company.ecommerce.inventory.domain.StockReservation;
import com.company.ecommerce.inventory.domain.event.StockReleasedEvent;
import com.company.ecommerce.inventory.infrastructure.mapper.InventoryMapper;
import com.company.ecommerce.inventory.infrastructure.persistence.InventoryRepository;
import com.company.ecommerce.inventory.infrastructure.persistence.InventoryTransactionRepository;
import com.company.ecommerce.inventory.infrastructure.persistence.StockReservationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ReleaseStockUseCaseTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private StockReservationRepository reservationRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private InventoryMapper inventoryMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private ReleaseStockUseCase useCase;

    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient()
                .when(transactionRepository.save(any(InventoryTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void releasesReservationAndPublishesEvent() {
        Inventory inventory = Inventory.create(productId, 10);
        inventory.reserve(4);
        StockReservation reservation = StockReservation.create(productId, 4, null);
        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        useCase.release(new ReleaseStockRequest(reservation.getId()));

        assertThat(inventory.getQuantityReserved()).isZero();
        verify(eventPublisher).publishEvent(any(StockReleasedEvent.class));
    }

    @Test
    void throwsWhenReservationMissing() {
        UUID reservationId = UUID.randomUUID();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.release(new ReleaseStockRequest(reservationId)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void throwsWhenReservationAlreadyReleased() {
        StockReservation reservation = StockReservation.create(productId, 4, null);
        reservation.release();
        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.of(Inventory.create(productId, 10)));

        assertThatThrownBy(() -> useCase.release(new ReleaseStockRequest(reservation.getId())))
                .isInstanceOf(BusinessException.class);
    }
}
