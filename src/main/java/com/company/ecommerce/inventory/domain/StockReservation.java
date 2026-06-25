package com.company.ecommerce.inventory.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import com.company.ecommerce.common.exception.BusinessException;
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
 * A reservation of stock for a product. Aggregate root owned by the {@code inventory} module.
 *
 * <p>{@code reference} optionally links the reservation to an external owner (e.g. a future cart or
 * order id), stored by value only.
 */
@Entity
@Table(name = "stock_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockReservation extends AuditableEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(name = "reference")
    private String reference;

    private StockReservation(UUID productId, int quantity, String reference) {
        this.productId = productId;
        this.quantity = quantity;
        this.reference = reference;
        this.status = ReservationStatus.ACTIVE;
    }

    public static StockReservation create(UUID productId, int quantity, String reference) {
        return new StockReservation(productId, quantity, reference);
    }

    /** Marks an active reservation as released, failing if it is already released. */
    public void release() {
        if (status != ReservationStatus.ACTIVE) {
            throw new BusinessException("Reservation is not active: " + getId());
        }
        this.status = ReservationStatus.RELEASED;
    }
}