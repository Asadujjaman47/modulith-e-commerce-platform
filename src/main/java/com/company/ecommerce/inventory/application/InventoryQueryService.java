package com.company.ecommerce.inventory.application;

import com.company.ecommerce.inventory.domain.Inventory;
import com.company.ecommerce.inventory.infrastructure.persistence.InventoryRepository;
import com.company.ecommerce.inventory.spi.InventoryQuery;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Default {@link InventoryQuery} implementation backed by the inventory repository. */
@Service
@RequiredArgsConstructor
public class InventoryQueryService implements InventoryQuery {

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional(readOnly = true)
    public int availableQuantity(UUID productId) {
        return inventoryRepository
                .findByProductId(productId)
                .map(Inventory::available)
                .orElse(0);
    }
}
