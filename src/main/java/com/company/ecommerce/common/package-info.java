/**
 * Shared cross-cutting building blocks (API response envelope, auditing base entity, exceptions).
 *
 * <p>Declared as an {@link org.springframework.modulith.ApplicationModule.Type#OPEN OPEN} module so
 * every business module may depend on its public types without violating module boundaries.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Common",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.company.ecommerce.common;