/**
 * Read-only query API exposed by the {@code user} module to other modules.
 *
 * <p>Published as a Spring Modulith named interface ({@code spi}) so modules such as {@code order}
 * may resolve a customer's address (to snapshot as a shipping address) without depending on user
 * internals.
 */
@org.springframework.modulith.NamedInterface("spi")
package com.company.ecommerce.user.spi;
