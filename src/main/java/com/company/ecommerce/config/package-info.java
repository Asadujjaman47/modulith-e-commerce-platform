/**
 * Application-wide technical configuration (security, Redis, OpenAPI, JPA auditing).
 *
 * <p>Declared {@link org.springframework.modulith.ApplicationModule.Type#OPEN OPEN} as it wires
 * framework concerns rather than owning business logic.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Config",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.company.ecommerce.config;