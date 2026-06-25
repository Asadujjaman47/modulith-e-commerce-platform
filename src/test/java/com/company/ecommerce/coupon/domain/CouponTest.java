package com.company.ecommerce.coupon.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ecommerce.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CouponTest {

    private final Instant now = Instant.now();

    private Coupon percentage(BigDecimal value, BigDecimal cap) {
        return Coupon.create(
                "SAVE",
                "desc",
                DiscountType.PERCENTAGE,
                value,
                null,
                cap,
                now.minus(Duration.ofDays(1)),
                now.plus(Duration.ofDays(1)),
                null);
    }

    @Test
    void calculatesPercentageDiscount() {
        Coupon coupon = percentage(new BigDecimal("20"), null);

        assertThat(coupon.calculateDiscount(new BigDecimal("150.00")))
                .isEqualByComparingTo("30.00");
    }

    @Test
    void capsPercentageDiscount() {
        Coupon coupon = percentage(new BigDecimal("50"), new BigDecimal("40.00"));

        assertThat(coupon.calculateDiscount(new BigDecimal("200.00")))
                .isEqualByComparingTo("40.00");
    }

    @Test
    void clampsFixedDiscountToOrderAmount() {
        Coupon coupon =
                Coupon.create(
                        "FLAT",
                        "desc",
                        DiscountType.FIXED_AMOUNT,
                        new BigDecimal("50.00"),
                        null,
                        null,
                        now.minus(Duration.ofDays(1)),
                        now.plus(Duration.ofDays(1)),
                        null);

        assertThat(coupon.calculateDiscount(new BigDecimal("30.00")))
                .isEqualByComparingTo("30.00");
    }

    @Test
    void rejectsBelowMinimumOrderAmount() {
        Coupon coupon =
                Coupon.create(
                        "MIN",
                        "desc",
                        DiscountType.PERCENTAGE,
                        new BigDecimal("10"),
                        new BigDecimal("100.00"),
                        null,
                        now.minus(Duration.ofDays(1)),
                        now.plus(Duration.ofDays(1)),
                        null);

        assertThatThrownBy(() -> coupon.validateFor(new BigDecimal("50.00"), now))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsExpiredCoupon() {
        Coupon coupon =
                Coupon.create(
                        "OLD",
                        "desc",
                        DiscountType.PERCENTAGE,
                        new BigDecimal("10"),
                        null,
                        null,
                        now.minus(Duration.ofDays(10)),
                        now.minus(Duration.ofDays(1)),
                        null);

        assertThat(coupon.isExpired(now)).isTrue();
        assertThatThrownBy(() -> coupon.validateFor(new BigDecimal("50.00"), now))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void enforcesUsageLimit() {
        Coupon coupon =
                Coupon.create(
                        "ONCE",
                        "desc",
                        DiscountType.PERCENTAGE,
                        new BigDecimal("10"),
                        null,
                        null,
                        now.minus(Duration.ofDays(1)),
                        now.plus(Duration.ofDays(1)),
                        1);

        coupon.recordUsage();

        assertThatThrownBy(() -> coupon.validateFor(new BigDecimal("50.00"), now))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(coupon::recordUsage).isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsInvalidValidityWindow() {
        assertThatThrownBy(
                        () ->
                                Coupon.create(
                                        "BAD",
                                        "desc",
                                        DiscountType.PERCENTAGE,
                                        new BigDecimal("10"),
                                        null,
                                        null,
                                        now.plus(Duration.ofDays(1)),
                                        now.minus(Duration.ofDays(1)),
                                        null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsPercentageAboveHundred() {
        assertThatThrownBy(() -> percentage(new BigDecimal("150"), null))
                .isInstanceOf(BusinessException.class);
    }
}
