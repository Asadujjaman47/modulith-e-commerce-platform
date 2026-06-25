package com.company.ecommerce.coupon.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Records a single application of a {@link Coupon} by a customer. Aggregate root owned by the
 * {@code coupon} module.
 *
 * <p>References the coupon and customer by id value only. {@code orderId} is left null until the
 * order module exists and links usages to concrete orders.
 */
@Entity
@Table(name = "coupon_usages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponUsage extends AuditableEntity {

    @Column(name = "coupon_id", nullable = false)
    private UUID couponId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    private CouponUsage(UUID couponId, UUID customerId, BigDecimal discountAmount) {
        this.couponId = couponId;
        this.customerId = customerId;
        this.discountAmount = discountAmount;
        this.usedAt = Instant.now();
    }

    public static CouponUsage record(UUID couponId, UUID customerId, BigDecimal discountAmount) {
        return new CouponUsage(couponId, customerId, discountAmount);
    }
}
