/**
 * Cart module: a customer's shopping cart and its items. (Phase 3)
 *
 * <p>Reads product details and stock via the catalog/inventory {@code spi} named interfaces and
 * keeps a price/name snapshot on each cart item. Exposes its own {@code spi} (read the cart, and a
 * clear command) so {@code order} can read the cart at checkout and clear it once an order is placed.
 * References products and customers by id value only (no cross-module FKs).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Cart")
package com.company.ecommerce.cart;