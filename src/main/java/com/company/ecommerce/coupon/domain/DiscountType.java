package com.company.ecommerce.coupon.domain;

/** How a coupon's {@code discountValue} is interpreted. */
public enum DiscountType {

    /** {@code discountValue} is a percentage (0–100) of the order amount. */
    PERCENTAGE,

    /** {@code discountValue} is a fixed monetary amount. */
    FIXED_AMOUNT
}
