package com.company.ecommerce.cart.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import com.company.ecommerce.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A line in a {@link Cart}. Child of the Cart aggregate — never referenced or persisted
 * independently of its owning cart.
 *
 * <p>References the catalog product by id value only and snapshots its {@code productName} and
 * {@code unitPrice} at the time it was added, so the cart total is stable even if the catalog price
 * later changes (the snapshot is refreshed on {@code ProductUpdatedEvent}).
 */
@Entity
@Table(name = "cart_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends AuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    CartItem(Cart cart, UUID productId, String productName, BigDecimal unitPrice, int quantity) {
        this.cart = cart;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        setQuantity(quantity);
    }

    /** Sets the absolute quantity for this line; must be positive. */
    void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("Quantity must be positive");
        }
        this.quantity = quantity;
    }

    void increaseQuantity(int delta) {
        setQuantity(this.quantity + delta);
    }

    void refreshProduct(String productName, BigDecimal unitPrice) {
        this.productName = productName;
        this.unitPrice = unitPrice;
    }

    /** Line total = unit price × quantity. */
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
