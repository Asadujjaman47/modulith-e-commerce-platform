package com.company.ecommerce.coupon.spi;

import java.math.BigDecimal;

/**
 * Read-only result of quoting a coupon against an order amount: the discount it would yield. Carries
 * no domain types so consumers (e.g. {@code order}) stay decoupled from coupon internals.
 */
public record CouponQuote(String code, BigDecimal discountAmount) {}
