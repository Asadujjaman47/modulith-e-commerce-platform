/**
 * Domain events published by the {@code auth} module.
 *
 * <p>Exposed as a Spring Modulith named interface ({@code events}) so other modules (e.g.
 * {@code user}, {@code notification}) may consume them without depending on auth internals.
 */
@org.springframework.modulith.NamedInterface("events")
package com.company.ecommerce.auth.domain.event;