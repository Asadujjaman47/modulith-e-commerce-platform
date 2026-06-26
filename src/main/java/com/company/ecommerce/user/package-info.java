/**
 * User module: customer profile and addresses. (Phase 1)
 *
 * <p>Exposes a read-only {@code spi} named interface so modules such as {@code order} can resolve a
 * customer's address without depending on user internals.
 */
@org.springframework.modulith.ApplicationModule(displayName = "User")
package com.company.ecommerce.user;