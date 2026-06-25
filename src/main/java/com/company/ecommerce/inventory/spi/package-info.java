/**
 * Read-only query API exposed by the {@code inventory} module to other modules.
 *
 * <p>Published as a Spring Modulith named interface ({@code spi}) so modules such as {@code cart}
 * may check available stock without depending on inventory internals.
 */
@org.springframework.modulith.NamedInterface("spi")
package com.company.ecommerce.inventory.spi;
