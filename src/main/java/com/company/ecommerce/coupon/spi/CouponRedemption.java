package com.company.ecommerce.coupon.spi;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command API for redeeming a coupon against a placed order, on behalf of other modules (e.g.
 * {@code order}). Implemented inside the {@code coupon} module.
 *
 * <p>Records the usage linked to the order, enforcing the coupon's usage limit, and publishes the
 * coupon's applied event. Separate from {@link CouponQuery#quote}, which only computes a discount.
 */
public interface CouponRedemption {

    /** Records a redemption of the coupon by a customer for the given order. */
    void redeem(String code, UUID customerId, UUID orderId, BigDecimal discountAmount);
}
