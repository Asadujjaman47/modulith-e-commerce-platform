/**
 * Cart module: a customer's shopping cart and its items. (Phase 3)
 *
 * <p>Reads product details and stock via the catalog/inventory {@code spi} named interfaces and
 * keeps a price/name snapshot on each cart item. References products and customers by id value
 * only (no cross-module FKs).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Cart")
package com.company.ecommerce.cart;