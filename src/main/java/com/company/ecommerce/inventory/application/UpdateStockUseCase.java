package com.company.ecommerce.inventory.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.inventory.api.dto.InventoryResponse;
import com.company.ecommerce.inventory.api.dto.UpdateStockRequest;
import com.company.ecommerce.inventory.domain.Inventory;
import com.company.ecommerce.inventory.domain.InventoryTransaction;
import com.company.ecommerce.inventory.domain.InventoryTransactionType;
import com.company.ecommerce.inventory.domain.event.StockUpdatedEvent;
import com.company.ecommerce.inventory.infrastructure.mapper.InventoryMapper;
import com.company.ecommerce.inventory.infrastructure.persistence.InventoryRepository;
import com.company.ecommerce.inventory.infrastructure.persistence.InventoryTransactionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sets a product's absolute on-hand quantity and records the adjustment. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateStockUseCase {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final InventoryMapper inventoryMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InventoryResponse update(UUID productId, UpdateStockRequest request) {
        Inventory inventory =
                inventoryRepository
                        .findByProductId(productId)
                        .orElseThrow(() -> new EntityNotFoundException("Inventory", productId));

        int previousOnHand = inventory.getQuantityOnHand();
        inventory.setOnHand(request.quantityOnHand());
        int delta = inventory.getQuantityOnHand() - previousOnHand;

        transactionRepository.save(
                InventoryTransaction.of(
                        productId, InventoryTransactionType.ADJUSTMENT, delta, request.reason()));
        eventPublisher.publishEvent(
                new StockUpdatedEvent(
                        productId, inventory.getQuantityOnHand(), inventory.available()));
        log.info(
                "Stock updated. productId={} onHand={} available={}",
                productId,
                inventory.getQuantityOnHand(),
                inventory.available());
        return inventoryMapper.toResponse(inventory);
    }
}