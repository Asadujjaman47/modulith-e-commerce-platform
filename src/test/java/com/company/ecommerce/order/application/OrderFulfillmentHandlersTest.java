package com.company.ecommerce.order.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.company.ecommerce.cart.spi.CartMaintenance;
import com.company.ecommerce.coupon.spi.CouponRedemption;
import com.company.ecommerce.inventory.spi.StockReservations;
import com.company.ecommerce.order.domain.event.OrderCancelledEvent;
import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.order.domain.event.OrderLine;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderFulfillmentHandlersTest {

    @Mock private StockReservations stockReservations;
    @Mock private CartMaintenance cartMaintenance;
    @Mock private CouponRedemption couponRedemption;
    @InjectMocks private OrderFulfillmentHandlers handlers;

    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    private OrderCreatedEvent created(String couponCode) {
        return new OrderCreatedEvent(
                orderId,
                "ORD-1",
                customerId,
                List.of(new OrderLine(productId, 2)),
                couponCode,
                new BigDecimal("20.00"),
                new BigDecimal("180.00"));
    }

    @Test
    void reservesStockPerLine() {
        handlers.reserveStock(created(null));

        verify(stockReservations).reserve(productId, 2, orderId.toString());
    }

    @Test
    void clearsCart() {
        handlers.clearCart(created(null));

        verify(cartMaintenance).clear(customerId);
    }

    @Test
    void redeemsCouponWhenPresent() {
        handlers.redeemCoupon(created("SAVE20"));

        verify(couponRedemption)
                .redeem(eq("SAVE20"), eq(customerId), eq(orderId), any(BigDecimal.class));
    }

    @Test
    void skipsCouponRedemptionWhenAbsent() {
        handlers.redeemCoupon(created(null));

        verify(couponRedemption, never()).redeem(any(), any(), any(), any());
    }

    @Test
    void releasesStockOnCancellation() {
        handlers.releaseStock(new OrderCancelledEvent(orderId, customerId, List.of()));

        verify(stockReservations).releaseByReference(orderId.toString());
    }
}
