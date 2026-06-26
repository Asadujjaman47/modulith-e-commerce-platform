package com.company.ecommerce.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.OrderStatus;
import com.company.ecommerce.order.infrastructure.persistence.OrderRepository;
import com.company.ecommerce.order.spi.OrderView;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock private OrderRepository orderRepository;
    @InjectMocks private OrderQueryService service;

    @Test
    void mapsOrderToViewWithAddress() {
        UUID customerId = UUID.randomUUID();
        Order order = Order.place("ORD-1", customerId, "USD", null, BigDecimal.ZERO, null);
        order.addItem(UUID.randomUUID(), "Widget", new BigDecimal("10.00"), 2);
        order.setShippingAddress("Home", "221B Baker St", null, "London", null, "NW1 6XE", "GB");
        order.recalculateTotals();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        OrderView view = service.findById(order.getId()).orElseThrow();

        assertThat(view.customerId()).isEqualTo(customerId);
        assertThat(view.status()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(view.currency()).isEqualTo("USD");
        assertThat(view.totalAmount()).isEqualByComparingTo("20.00");
        assertThat(view.addressLine1()).isEqualTo("221B Baker St");
        assertThat(view.addressCity()).isEqualTo("London");
        assertThat(view.addressCountry()).isEqualTo("GB");
    }

    @Test
    void returnsEmptyWhenMissing() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(service.findById(id)).isEmpty();
    }
}
