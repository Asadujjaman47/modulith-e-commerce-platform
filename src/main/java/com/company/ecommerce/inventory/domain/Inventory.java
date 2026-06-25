package com.company.ecommerce.inventory.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import com.company.ecommerce.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stock record for a single product. Aggregate root owned by the {@code inventory} module.
 *
 * <p>References the catalog product by id value only (no cross-module FK). Tracks on-hand and
 * reserved quantities; available stock is the difference and may never go negative.
 */
@Entity
@Table(name = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends AuditableEntity {

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    private Inventory(UUID productId, int quantityOnHand) {
        this.productId = productId;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = 0;
    }

    public static Inventory create(UUID productId, int quantityOnHand) {
        return new Inventory(productId, quantityOnHand);
    }

    /** Available = on-hand minus reserved. */
    public int available() {
        return quantityOnHand - quantityReserved;
    }

    /** Sets the absolute on-hand quantity; must remain at least the currently reserved amount. */
    public void setOnHand(int newOnHand) {
        if (newOnHand < 0) {
            throw new BusinessException("On-hand quantity cannot be negative");
        }
        if (newOnHand < quantityReserved) {
            throw new BusinessException(
                    "On-hand quantity (%d) cannot be less than reserved (%d)"
                            .formatted(newOnHand, quantityReserved));
        }
        this.quantityOnHand = newOnHand;
    }

    /** Reserves {@code quantity} units, failing if insufficient stock is available. */
    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("Reservation quantity must be positive");
        }
        if (quantity > available()) {
            throw new BusinessException(
                    "Insufficient stock for product %s: requested %d, available %d"
                            .formatted(productId, quantity, available()));
        }
        this.quantityReserved += quantity;
    }

    /** Releases {@code quantity} previously reserved units back to available stock. */
    public void release(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("Release quantity must be positive");
        }
        if (quantity > quantityReserved) {
            throw new BusinessException(
                    "Cannot release %d units; only %d reserved"
                            .formatted(quantity, quantityReserved));
        }
        this.quantityReserved -= quantity;
    }
}