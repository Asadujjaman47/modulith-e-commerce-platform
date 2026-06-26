package com.company.ecommerce.order.domain;

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
 * A line in an {@link Order}. Child of the Order aggregate — never referenced or persisted
 * independently of its owning order.
 *
 * <p>References the catalog product by id value only and snapshots its {@code productName} and
 * {@code unitPrice} at placement time, so the order total is permanent even if the catalog price
 * later changes.
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends AuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    OrderItem(Order order, UUID productId, String productName, BigDecimal unitPrice, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("Quantity must be positive");
        }
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    /** Line total = unit price × quantity. */
    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
