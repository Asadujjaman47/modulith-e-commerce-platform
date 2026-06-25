package com.company.ecommerce.inventory.application;

import com.company.ecommerce.catalog.domain.event.ProductCreatedEvent;
import com.company.ecommerce.inventory.domain.Inventory;
import com.company.ecommerce.inventory.infrastructure.persistence.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;

/**
 * Seeds a zero-stock {@link Inventory} record whenever the {@code catalog} module reports a new
 * product. This is the inventory module's only dependency on catalog (a consumed event), in line
 * with the allowed {@code inventory -> catalog} dependency.
 */
@Slf4j
@RequiredArgsConstructor
@org.springframework.stereotype.Component
public class ProductCreatedListener {

    private final InventoryRepository inventoryRepository;

    @ApplicationModuleListener
    public void on(ProductCreatedEvent event) {
        if (inventoryRepository.existsByProductId(event.productId())) {
            return;
        }
        inventoryRepository.save(Inventory.create(event.productId(), 0));
        log.info("Inventory seeded for product. productId={} sku={}", event.productId(), event.sku());
    }
}