package com.company.ecommerce.inventory.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.inventory.api.dto.InventoryResponse;
import com.company.ecommerce.inventory.infrastructure.mapper.InventoryMapper;
import com.company.ecommerce.inventory.infrastructure.persistence.InventoryRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads the stock levels for a product. */
@Service
@RequiredArgsConstructor
public class GetStockUseCase {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Transactional(readOnly = true)
    public InventoryResponse getByProductId(UUID productId) {
        return inventoryRepository
                .findByProductId(productId)
                .map(inventoryMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Inventory", productId));
    }
}