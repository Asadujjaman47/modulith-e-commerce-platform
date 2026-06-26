package com.company.ecommerce.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ecommerce.common.exception.BusinessException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    private final UUID customerId = UUID.randomUUID();

    private Order orderWithItems(BigDecimal discount) {
        Order order = Order.place("ORD-1", customerId, "USD", discount.signum() > 0 ? "SAVE" : null,
                discount, null);
        order.addItem(UUID.randomUUID(), "Widget", new BigDecimal("10.00"), 2);
        order.addItem(UUID.randomUUID(), "Gadget", new BigDecimal("5.50"), 1);
        return order;
    }

    @Test
    void recalculatesSubtotalAndTotal() {
        Order order = orderWithItems(new BigDecimal("4.00"));

        order.recalculateTotals();

        assertThat(order.getSubtotal()).isEqualByComparingTo("25.50");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("21.50");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void totalNeverGoesNegative() {
        Order order = orderWithItems(new BigDecimal("100.00"));

        order.recalculateTotals();

        assertThat(order.getTotalAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void recalculateRejectsEmptyOrder() {
        Order order = Order.place("ORD-1", customerId, "USD", null, BigDecimal.ZERO, null);

        assertThatThrownBy(order::recalculateTotals).isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelsFromPending() {
        Order order = orderWithItems(BigDecimal.ZERO);
        order.recalculateTotals();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull();
    }

    @Test
    void cannotCancelShippedOrder() {
        Order order = orderWithItems(BigDecimal.ZERO);
        order.transitionTo(OrderStatus.PROCESSING);
        order.transitionTo(OrderStatus.SHIPPED);

        assertThatThrownBy(order::cancel).isInstanceOf(BusinessException.class);
    }

    @Test
    void allowsLegalTransition() {
        Order order = orderWithItems(BigDecimal.ZERO);

        order.transitionTo(OrderStatus.PROCESSING);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void rejectsIllegalTransition() {
        Order order = orderWithItems(BigDecimal.ZERO);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.DELIVERED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsTransitionToSameStatus() {
        Order order = orderWithItems(BigDecimal.ZERO);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PENDING))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void transitionToCancelledStampsCancelledAt() {
        Order order = orderWithItems(BigDecimal.ZERO);

        order.transitionTo(OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledAt()).isNotNull();
    }
}
