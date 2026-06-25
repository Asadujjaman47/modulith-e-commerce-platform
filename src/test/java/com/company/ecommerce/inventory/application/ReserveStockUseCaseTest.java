package com.company.ecommerce.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.inventory.api.dto.ReserveStockRequest;
import com.company.ecommerce.inventory.domain.Inventory;
import com.company.ecommerce.inventory.domain.InventoryTransaction;
import com.company.ecommerce.inventory.domain.StockReservation;
import com.company.ecommerce.inventory.domain.event.StockReservedEvent;
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
class ReserveStockUseCaseTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private StockReservationRepository reservationRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private InventoryMapper inventoryMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private ReserveStockUseCase useCase;

    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient()
                .when(reservationRepository.save(any(StockReservation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient()
                .when(transactionRepository.save(any(InventoryTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reservesStockAndPublishesEvent() {
        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.of(Inventory.create(productId, 10)));

        useCase.reserve(new ReserveStockRequest(productId, 4, "cart-1"));

        verify(reservationRepository).save(any(StockReservation.class));
        verify(transactionRepository).save(any(InventoryTransaction.class));
        verify(eventPublisher).publishEvent(any(StockReservedEvent.class));
    }

    @Test
    void throwsWhenInventoryMissing() {
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.reserve(new ReserveStockRequest(productId, 1, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void throwsAndReservesNothingWhenInsufficient() {
        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.of(Inventory.create(productId, 2)));

        assertThatThrownBy(() -> useCase.reserve(new ReserveStockRequest(productId, 5, null)))
                .isInstanceOf(BusinessException.class);
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void reservationIncrementsReservedQuantity() {
        Inventory inventory = Inventory.create(productId, 10);
        when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

        useCase.reserve(new ReserveStockRequest(productId, 3, null));

        assertThat(inventory.getQuantityReserved()).isEqualTo(3);
    }
}
