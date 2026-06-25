package com.company.ecommerce.coupon.domain.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Published when a coupon is successfully applied by a customer. */
public record CouponAppliedEvent(
        UUID couponId, String code, UUID customerId, BigDecimal discountAmount) {}
