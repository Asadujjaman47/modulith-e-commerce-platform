/**
 * Domain events published by the {@code review} module.
 *
 * <p>Exposed as a Spring Modulith named interface ({@code events}) so other modules (e.g.
 * {@code reporting}/{@code audit} in later phases) may consume them without depending on review
 * internals.
 */
@org.springframework.modulith.NamedInterface("events")
package com.company.ecommerce.review.domain.event;
