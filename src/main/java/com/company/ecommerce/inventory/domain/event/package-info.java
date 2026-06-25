/**
 * Domain events published by the {@code inventory} module.
 *
 * <p>Exposed as a Spring Modulith named interface ({@code events}) so other modules (e.g.
 * {@code cart}, {@code notification}) may consume them without depending on inventory internals.
 */
@org.springframework.modulith.NamedInterface("events")
package com.company.ecommerce.inventory.domain.event;