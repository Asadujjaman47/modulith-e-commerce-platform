package com.company.ecommerce.order.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import com.company.ecommerce.common.exception.BusinessException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Customer order aggregate root. Owned by the {@code order} module.
 *
 * <p>Created from a snapshot of the customer's cart at placement time; {@link OrderItem}s and the
 * {@link OrderAddress} are children managed exclusively through this aggregate. References the
 * customer, products and (optionally) a coupon by id/value only — no cross-module FKs. Status changes
 * are guarded by the {@link OrderStatus} state machine.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends AuditableEntity {

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private OrderAddress shippingAddress;

    private Order(
            String orderNumber,
            UUID customerId,
            String currency,
            String couponCode,
            BigDecimal discountAmount,
            String idempotencyKey) {
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.currency = currency;
        this.couponCode = couponCode;
        this.discountAmount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        this.idempotencyKey = idempotencyKey;
        this.status = OrderStatus.PENDING;
        this.placedAt = Instant.now();
    }

    /**
     * Starts a new {@code PENDING} order. Add line items and the shipping address, then call
     * {@link #recalculateTotals()} before persisting.
     */
    public static Order place(
            String orderNumber,
            UUID customerId,
            String currency,
            String couponCode,
            BigDecimal discountAmount,
            String idempotencyKey) {
        return new Order(orderNumber, customerId, currency, couponCode, discountAmount, idempotencyKey);
    }

    /** Adds a line item snapshotting the product name and unit price. */
    public OrderItem addItem(UUID productId, String productName, BigDecimal unitPrice, int quantity) {
        OrderItem item = new OrderItem(this, productId, productName, unitPrice, quantity);
        items.add(item);
        return item;
    }

    /** Snapshots the shipping address for this order. */
    public void setShippingAddress(
            String label,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country) {
        this.shippingAddress =
                new OrderAddress(this, label, line1, line2, city, state, postalCode, country);
    }

    /**
     * Recomputes {@code subtotal} (sum of line totals) and {@code totalAmount} (subtotal minus the
     * discount, never below zero). Must be called after items and discount are set.
     */
    public void recalculateTotals() {
        if (items.isEmpty()) {
            throw new BusinessException("An order must contain at least one item");
        }
        this.subtotal =
                items.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = subtotal.subtract(discountAmount);
        this.totalAmount = total.signum() < 0 ? BigDecimal.ZERO : total;
    }

    /** Cancels the order, failing if its current status does not allow cancellation. */
    public void cancel() {
        if (!status.isCancellable()) {
            throw new BusinessException("Order cannot be cancelled in status " + status);
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    /** Transitions the order to {@code target}, enforcing the allowed-transition rules. */
    public void transitionTo(OrderStatus target) {
        if (status == target) {
            throw new BusinessException("Order is already in status " + status);
        }
        if (!status.canTransitionTo(target)) {
            throw new BusinessException(
                    "Illegal order status transition: %s -> %s".formatted(status, target));
        }
        this.status = target;
        if (target == OrderStatus.CANCELLED) {
            this.cancelledAt = Instant.now();
        }
    }
}
