/**
 * Read-only query API exposed by the {@code cart} module to other modules.
 *
 * <p>Published as a Spring Modulith named interface ({@code spi}) so modules such as {@code order}
 * may read the customer's cart contents at checkout without depending on cart internals.
 */
@org.springframework.modulith.NamedInterface("spi")
package com.company.ecommerce.cart.spi;
