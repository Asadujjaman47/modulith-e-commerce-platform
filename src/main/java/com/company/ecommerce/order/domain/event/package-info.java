/**
 * Domain events published by the {@code order} module.
 *
 * <p>Exposed as a Spring Modulith named interface ({@code events}) so other modules (e.g.
 * {@code inventory}, {@code coupon}, {@code cart}, and later {@code payment}/{@code notification}/
 * {@code audit}) may consume them without depending on order internals.
 */
@org.springframework.modulith.NamedInterface("events")
package com.company.ecommerce.order.domain.event;
