package com.company.ecommerce.coupon.spi;

import java.math.BigDecimal;

/**
 * Synchronous, read-only API to quote a coupon. Implemented inside the {@code coupon} module and
 * consumed by modules allowed to depend on coupon (e.g. {@code order} at checkout).
 *
 * <p>Quoting performs no writes and records no usage — usage is recorded asynchronously by the coupon
 * module when it consumes {@code OrderCreatedEvent}.
 */
public interface CouponQuery {

    /**
     * Validates the coupon against {@code orderAmount} and returns the discount it would yield.
     * Throws if the code is unknown (404) or cannot be applied (409, e.g. inactive, expired,
     * usage-exhausted or below the minimum order amount).
     */
    CouponQuote quote(String code, BigDecimal orderAmount);
}
