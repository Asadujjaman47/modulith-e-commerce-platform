/**
 * Coupon module: discount codes, validation and usage tracking. (Phase 3)
 *
 * <p>Dependency-free: validation and application operate on an order amount supplied by the caller,
 * so the module never reaches into cart or order. Publishes {@code CouponAppliedEvent} /
 * {@code CouponExpiredEvent}; will consume {@code OrderCreatedEvent} once the order module exists.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Coupon")
package com.company.ecommerce.coupon;