package com.company.ecommerce.inventory.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Append-only ledger entry recording a change to a product's stock. Aggregate root owned by the
 * {@code inventory} module.
 *
 * <p>{@code quantityDelta} is signed from the perspective of available stock: negative when stock
 * is reserved or removed, positive when received, released or adjusted upward.
 */
@Entity
@Table(name = "inventory_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryTransaction extends AuditableEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private InventoryTransactionType type;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Column(name = "reason")
    private String reason;

    private InventoryTransaction(
            UUID productId, InventoryTransactionType type, int quantityDelta, String reason) {
        this.productId = productId;
        this.type = type;
        this.quantityDelta = quantityDelta;
        this.reason = reason;
    }

    public static InventoryTransaction of(
            UUID productId, InventoryTransactionType type, int quantityDelta, String reason) {
        return new InventoryTransaction(productId, type, quantityDelta, reason);
    }
}