/**
 * Coupon module: discount codes, validation and usage tracking. (Phase 3)
 *
 * <p>Validation and application operate on an order amount supplied by the caller, so the module
 * never reaches into cart or order aggregates. Exposes an {@code spi} for {@code order} to use at
 * checkout: a read-only quote (compute a discount) and a redeem command (record usage linked to a
 * placed order). Publishes {@code CouponAppliedEvent} / {@code CouponExpiredEvent}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Coupon")
package com.company.ecommerce.coupon;