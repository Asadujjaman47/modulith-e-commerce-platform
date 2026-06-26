/**
 * Domain events published by the {@code payment} module.
 *
 * <p>Exposed as a Spring Modulith named interface ({@code events}) so other modules (e.g.
 * {@code shipment}, and later {@code notification}/{@code reporting}/{@code audit}) may consume them
 * without depending on payment internals.
 */
@org.springframework.modulith.NamedInterface("events")
package com.company.ecommerce.payment.domain.event;
