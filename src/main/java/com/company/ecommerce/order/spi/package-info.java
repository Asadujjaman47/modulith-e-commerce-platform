/**
 * Service-provider API exposed by the {@code order} module to other modules.
 *
 * <p>Published as a Spring Modulith named interface ({@code spi}) so the {@code payment} and
 * {@code shipment} modules may read an order ({@link com.company.ecommerce.order.spi.OrderQuery}) and
 * advance its status ({@link com.company.ecommerce.order.spi.OrderLifecycle}) without depending on
 * order internals. The order module never depends back on payment/shipment, so the module graph stays
 * acyclic: status transitions are <em>pushed in</em> through this {@code spi} rather than order
 * consuming payment/shipment events.
 */
@org.springframework.modulith.NamedInterface("spi")
package com.company.ecommerce.order.spi;
