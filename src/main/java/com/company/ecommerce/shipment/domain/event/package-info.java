/**
 * Domain events published by the {@code shipment} module.
 *
 * <p>Exposed as a Spring Modulith named interface ({@code events}) so other modules (e.g. later
 * {@code notification}/{@code reporting}/{@code audit}) may consume them without depending on shipment
 * internals.
 */
@org.springframework.modulith.NamedInterface("events")
package com.company.ecommerce.shipment.domain.event;
