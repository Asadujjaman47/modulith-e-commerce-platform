package com.company.ecommerce.inventory.infrastructure.persistence;

import com.company.ecommerce.inventory.domain.InventoryTransaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link InventoryTransaction} ledger entries. */
public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, UUID> {}