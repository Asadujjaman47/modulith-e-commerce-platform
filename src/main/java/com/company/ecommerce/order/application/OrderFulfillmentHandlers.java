package com.company.ecommerce.order.application;

import com.company.ecommerce.cart.spi.CartMaintenance;
import com.company.ecommerce.coupon.spi.CouponRedemption;
import com.company.ecommerce.inventory.spi.StockReservations;
import com.company.ecommerce.order.domain.event.OrderCancelledEvent;
import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Applies an order's downstream side effects after it is placed or cancelled.
 *
 * <p>The order module orchestrates the modules it depends on (cart, coupon, inventory) via their
 * {@code spi} command interfaces. Each side effect is a separate {@link ApplicationModuleListener},
 * so it runs after the placing transaction commits and in its own transaction — keeping each write
 * within a single module's boundary.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderFulfillmentHandlers {

    private final StockReservations stockReservations;
    private final CartMaintenance cartMaintenance;
    private final CouponRedemption couponRedemption;

    @ApplicationModuleListener
    public void reserveStock(OrderCreatedEvent event) {
        String reference = event.orderId().toString();
        event.lines()
                .forEach(line -> stockReservations.reserve(line.productId(), line.quantity(), reference));
        log.info("Reserved stock for {} line(s) of order {}.", event.lines().size(), event.orderId());
    }

    @ApplicationModuleListener
    public void clearCart(OrderCreatedEvent event) {
        cartMaintenance.clear(event.customerId());
    }

    @ApplicationModuleListener
    public void redeemCoupon(OrderCreatedEvent event) {
        if (event.couponCode() == null) {
            return;
        }
        couponRedemption.redeem(
                event.couponCode(),
                event.customerId(),
                event.orderId(),
                event.discountAmount());
    }

    @ApplicationModuleListener
    public void releaseStock(OrderCancelledEvent event) {
        stockReservations.releaseByReference(event.orderId().toString());
        log.info("Released reserved stock for cancelled order {}.", event.orderId());
    }
}
