package com.company.ecommerce.reporting.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable per-order sales fact. Recorded once per placed order (unique {@code order_id}) from
 * {@code OrderCreatedEvent}. Aggregated by {@code order_date} to produce sales reports.
 *
 * <p>References the order and customer by id value only — no cross-module FKs.
 */
@Entity
@Table(name = "sales_facts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesFact extends AuditableEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "order_total", nullable = false)
    private BigDecimal orderTotal;

    @Column(name = "discount_total", nullable = false)
    private BigDecimal discountTotal;

    @Column(name = "item_count", nullable = false)
    private int itemCount;

    private SalesFact(
            UUID orderId,
            UUID customerId,
            LocalDate orderDate,
            BigDecimal orderTotal,
            BigDecimal discountTotal,
            int itemCount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.orderTotal = orderTotal;
        this.discountTotal = discountTotal;
        this.itemCount = itemCount;
    }

    /** Records a sales fact for a placed order. */
    public static SalesFact record(
            UUID orderId,
            UUID customerId,
            LocalDate orderDate,
            BigDecimal orderTotal,
            BigDecimal discountTotal,
            int itemCount) {
        return new SalesFact(orderId, customerId, orderDate, orderTotal, discountTotal, itemCount);
    }
}
