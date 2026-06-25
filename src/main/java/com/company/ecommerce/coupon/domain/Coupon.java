package com.company.ecommerce.coupon.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import com.company.ecommerce.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Discount coupon aggregate root. Owned by the {@code coupon} module.
 *
 * <p>Encapsulates its own validity and discount rules. Validation and discount calculation operate
 * on an order amount supplied by the caller, keeping the module free of cart/order dependencies.
 */
@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends AuditableEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount")
    private BigDecimal minOrderAmount;

    @Column(name = "max_discount_amount")
    private BigDecimal maxDiscountAmount;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "times_used", nullable = false)
    private int timesUsed;

    @Column(name = "active", nullable = false)
    private boolean active;

    private Coupon(
            String code,
            String description,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minOrderAmount,
            BigDecimal maxDiscountAmount,
            Instant validFrom,
            Instant validUntil,
            Integer usageLimit) {
        if (validUntil.isBefore(validFrom)) {
            throw new BusinessException("Coupon validUntil must be after validFrom");
        }
        if (discountValue.signum() <= 0) {
            throw new BusinessException("Discount value must be positive");
        }
        if (discountType == DiscountType.PERCENTAGE
                && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("Percentage discount cannot exceed 100");
        }
        this.code = code;
        this.description = description;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.usageLimit = usageLimit;
        this.timesUsed = 0;
        this.active = true;
    }

    public static Coupon create(
            String code,
            String description,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minOrderAmount,
            BigDecimal maxDiscountAmount,
            Instant validFrom,
            Instant validUntil,
            Integer usageLimit) {
        return new Coupon(
                code,
                description,
                discountType,
                discountValue,
                minOrderAmount,
                maxDiscountAmount,
                validFrom,
                validUntil,
                usageLimit);
    }

    /** True when {@code now} falls outside the coupon's validity window. */
    public boolean isExpired(Instant now) {
        return now.isAfter(validUntil);
    }

    /**
     * Verifies the coupon may be applied to an order of {@code orderAmount} at {@code now}, throwing
     * a {@link BusinessException} (HTTP 409) describing the first rule violated.
     */
    public void validateFor(BigDecimal orderAmount, Instant now) {
        if (!active) {
            throw new BusinessException("Coupon is not active: " + code);
        }
        if (now.isBefore(validFrom)) {
            throw new BusinessException("Coupon is not yet valid: " + code);
        }
        if (now.isAfter(validUntil)) {
            throw new BusinessException("Coupon has expired: " + code);
        }
        if (usageLimit != null && timesUsed >= usageLimit) {
            throw new BusinessException("Coupon usage limit reached: " + code);
        }
        if (minOrderAmount != null && orderAmount.compareTo(minOrderAmount) < 0) {
            throw new BusinessException(
                    "Order amount %s is below the minimum %s for coupon %s"
                            .formatted(orderAmount, minOrderAmount, code));
        }
    }

    /**
     * Computes the discount for {@code orderAmount}. Percentage discounts are capped at
     * {@code maxDiscountAmount} when set; fixed discounts never exceed the order amount. The result
     * is rounded to two decimal places.
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        BigDecimal discount =
                switch (discountType) {
                    case PERCENTAGE -> {
                        BigDecimal raw =
                                orderAmount
                                        .multiply(discountValue)
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        yield (maxDiscountAmount != null && raw.compareTo(maxDiscountAmount) > 0)
                                ? maxDiscountAmount
                                : raw;
                    }
                    case FIXED_AMOUNT -> discountValue.min(orderAmount);
                };
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    /** Records a single use, enforcing the usage limit. */
    public void recordUsage() {
        if (usageLimit != null && timesUsed >= usageLimit) {
            throw new BusinessException("Coupon usage limit reached: " + code);
        }
        this.timesUsed++;
    }

    /** Deactivates the coupon (e.g. once detected expired). */
    public void deactivate() {
        this.active = false;
    }
}
