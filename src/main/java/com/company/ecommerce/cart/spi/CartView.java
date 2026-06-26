package com.company.ecommerce.cart.spi;

import java.math.BigDecimal;
import java.util.List;

/** Read-only projection of a customer's cart for cross-module consumers (e.g. {@code order}). */
public record CartView(List<CartLineView> items, BigDecimal subtotal) {

    /** Whether the cart has no line items. */
    public boolean isEmpty() {
        return items.isEmpty();
    }
}
