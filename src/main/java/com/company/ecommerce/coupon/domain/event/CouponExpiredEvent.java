package com.company.ecommerce.coupon.domain.event;

import java.util.UUID;

/** Published when an active coupon is found to have expired and is deactivated. */
public record CouponExpiredEvent(UUID couponId, String code) {}
