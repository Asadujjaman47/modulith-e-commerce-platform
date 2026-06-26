package com.company.ecommerce.reporting.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable per-order-line sales fact. Recorded once per (order, product) pair from the lines carried
 * on {@code OrderCreatedEvent}. Aggregated by {@code product_id} to produce product reports.
 *
 * <p>References the order and product by id value only — no cross-module FKs. Order events carry only
 * product id and quantity (no unit price), so product reports cover units sold and order counts, not
 * per-product revenue.
 */
@Entity
@Table(name = "product_sales_facts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSalesFact extends AuditableEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    private ProductSalesFact(UUID orderId, UUID productId, int quantity, LocalDate orderDate) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.orderDate = orderDate;
    }

    /** Records a product sales fact for one line of a placed order. */
    public static ProductSalesFact record(
            UUID orderId, UUID productId, int quantity, LocalDate orderDate) {
        return new ProductSalesFact(orderId, productId, quantity, orderDate);
    }
}
