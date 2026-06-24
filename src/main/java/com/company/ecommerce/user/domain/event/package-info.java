/**
 * Domain events published by the {@code user} module, exposed as a Spring Modulith named interface
 * ({@code events}) for consumption by other modules (e.g. {@code notification}, {@code audit}).
 */
@org.springframework.modulith.NamedInterface("events")
package com.company.ecommerce.user.domain.event;