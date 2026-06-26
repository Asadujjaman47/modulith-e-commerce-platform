package com.company.ecommerce.cart.spi;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only projection of a single cart line for cross-module consumers (e.g. {@code order}). Carries
 * the catalog product id and the price/name snapshot taken when the item was added.
 */
public record CartLineView(UUID productId, String productName, BigDecimal unitPrice, int quantity) {}
