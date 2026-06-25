package com.company.ecommerce.inventory.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.inventory.api.dto.ReleaseStockRequest;
import com.company.ecommerce.inventory.api.dto.ReservationResponse;
import com.company.ecommerce.inventory.domain.Inventory;
import com.company.ecommerce.inventory.domain.InventoryTransaction;
import com.company.ecommerce.inventory.domain.InventoryTransactionType;
import com.company.ecommerce.inventory.domain.StockReservation;
import com.company.ecommerce.inventory.domain.event.StockReleasedEvent;
import com.company.ecommerce.inventory.infrastructure.mapper.InventoryMapper;
import com.company.ecommerce.inventory.infrastructure.persistence.InventoryRepository;
import com.company.ecommerce.inventory.infrastructure.persistence.InventoryTransactionRepository;
import com.company.ecommerce.inventory.infrastructure.persistence.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Releases a previously created stock reservation, returning the units to available stock. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseStockUseCase {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryMapper inventoryMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReservationResponse release(ReleaseStockRequest request) {
        StockReservation reservation =
                reservationRepository
                        .findById(request.reservationId())
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "StockReservation", request.reservationId()));
        Inventory inventory =
                inventoryRepository
                        .findByProductId(reservation.getProductId())
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Inventory", reservation.getProductId()));

        reservation.release();
        inventory.release(reservation.getQuantity());
        transactionRepository.save(
                InventoryTransaction.of(
                        reservation.getProductId(),
                        InventoryTransactionType.RELEASE,
                        reservation.getQuantity(),
                        "Release " + reservation.getId()));

        eventPublisher.publishEvent(
                new StockReleasedEvent(
                        reservation.getId(),
                        reservation.getProductId(),
                        reservation.getQuantity()));
        log.info(
                "Stock released. reservationId={} productId={} quantity={}",
                reservation.getId(),
                reservation.getProductId(),
                reservation.getQuantity());
        return inventoryMapper.toResponse(reservation);
    }
}