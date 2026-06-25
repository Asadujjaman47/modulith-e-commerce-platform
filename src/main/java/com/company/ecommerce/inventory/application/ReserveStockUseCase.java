package com.company.ecommerce.inventory.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.inventory.api.dto.ReservationResponse;
import com.company.ecommerce.inventory.api.dto.ReserveStockRequest;
import com.company.ecommerce.inventory.domain.Inventory;
import com.company.ecommerce.inventory.domain.InventoryTransaction;
import com.company.ecommerce.inventory.domain.InventoryTransactionType;
import com.company.ecommerce.inventory.domain.StockReservation;
import com.company.ecommerce.inventory.domain.event.StockReservedEvent;
import com.company.ecommerce.inventory.infrastructure.mapper.InventoryMapper;
import com.company.ecommerce.inventory.infrastructure.persistence.InventoryRepository;
import com.company.ecommerce.inventory.infrastructure.persistence.InventoryTransactionRepository;
import com.company.ecommerce.inventory.infrastructure.persistence.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reserves stock for a product, failing with a business error if insufficient stock is available. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReserveStockUseCase {

    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryMapper inventoryMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ReservationResponse reserve(ReserveStockRequest request) {
        Inventory inventory =
                inventoryRepository
                        .findByProductId(request.productId())
                        .orElseThrow(
                                () -> new EntityNotFoundException("Inventory", request.productId()));

        inventory.reserve(request.quantity());
        StockReservation reservation =
                reservationRepository.save(
                        StockReservation.create(
                                request.productId(), request.quantity(), request.reference()));
        transactionRepository.save(
                InventoryTransaction.of(
                        request.productId(),
                        InventoryTransactionType.RESERVE,
                        -request.quantity(),
                        "Reservation " + reservation.getId()));

        eventPublisher.publishEvent(
                new StockReservedEvent(
                        reservation.getId(), request.productId(), request.quantity()));
        log.info(
                "Stock reserved. reservationId={} productId={} quantity={}",
                reservation.getId(),
                request.productId(),
                request.quantity());
        return inventoryMapper.toResponse(reservation);
    }
}