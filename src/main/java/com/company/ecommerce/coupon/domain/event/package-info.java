/**
 * Domain events published by the {@code coupon} module.
 *
 * <p>Exposed as a Spring Modulith named interface ({@code events}) so other modules (e.g.
 * {@code order}, {@code notification}, {@code audit}) may consume them without depending on coupon
 * internals.
 */
@org.springframework.modulith.NamedInterface("events")
package com.company.ecommerce.coupon.domain.event;
