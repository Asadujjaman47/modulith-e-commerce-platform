package com.company.ecommerce.inventory.infrastructure.persistence;

import com.company.ecommerce.inventory.domain.Inventory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Inventory} aggregates. */
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    boolean existsByProductId(UUID productId);
}