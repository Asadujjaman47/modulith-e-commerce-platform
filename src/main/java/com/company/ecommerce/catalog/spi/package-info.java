/**
 * Read-only query API exposed by the {@code catalog} module to other modules.
 *
 * <p>Published as a Spring Modulith named interface ({@code spi}) so modules such as {@code cart}
 * may look up product details (name, price, active flag) without depending on catalog internals.
 */
@org.springframework.modulith.NamedInterface("spi")
package com.company.ecommerce.catalog.spi;
