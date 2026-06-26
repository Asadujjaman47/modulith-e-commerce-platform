/**
 * Read-only query API exposed by the {@code coupon} module to other modules.
 *
 * <p>Published as a Spring Modulith named interface ({@code spi}) so modules such as {@code order}
 * may quote a coupon (compute its discount) without depending on coupon internals and without
 * recording usage.
 */
@org.springframework.modulith.NamedInterface("spi")
package com.company.ecommerce.coupon.spi;
