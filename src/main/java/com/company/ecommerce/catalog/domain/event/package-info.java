/**
 * Domain events published by the {@code catalog} module.
 *
 * <p>Exposed as a Spring Modulith named interface ({@code events}) so other modules (e.g.
 * {@code inventory}) may consume them without depending on catalog internals.
 */
@org.springframework.modulith.NamedInterface("events")
package com.company.ecommerce.catalog.domain.event;