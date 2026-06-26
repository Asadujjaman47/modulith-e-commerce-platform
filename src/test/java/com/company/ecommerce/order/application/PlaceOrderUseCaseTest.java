package com.company.ecommerce.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.cart.spi.CartLineView;
import com.company.ecommerce.cart.spi.CartQuery;
import com.company.ecommerce.cart.spi.CartView;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.coupon.spi.CouponQuery;
import com.company.ecommerce.coupon.spi.CouponQuote;
import com.company.ecommerce.inventory.spi.InventoryQuery;
import com.company.ecommerce.order.api.dto.PlaceOrderRequest;
import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.order.infrastructure.mapper.OrderMapper;
import com.company.ecommerce.order.infrastructure.persistence.OrderRepository;
import com.company.ecommerce.user.spi.AddressView;
import com.company.ecommerce.user.spi.UserQuery;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlaceOrderUseCaseTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderNumberGenerator orderNumberGenerator;
    @Mock private CartQuery cartQuery;
    @Mock private UserQuery userQuery;
    @Mock private CouponQuery couponQuery;
    @Mock private InventoryQuery inventoryQuery;
    @Mock private OrderMapper orderMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private PlaceOrderUseCase useCase;

    private final UUID customerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID addressId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(orderNumberGenerator.generate()).thenReturn("ORD-TEST-1");
        when(userQuery.findAddress(customerId, addressId))
                .thenReturn(
                        Optional.of(
                                new AddressView(
                                        addressId, "Home", "221B Baker St", null, "London", null,
                                        "NW1", "GB")));
    }

    private CartView cart() {
        return new CartView(
                List.of(new CartLineView(productId, "UltraBook", new BigDecimal("100.00"), 2)),
                new BigDecimal("200.00"));
    }

    @Test
    void placesOrderFromCart() {
        when(cartQuery.findCart(customerId)).thenReturn(Optional.of(cart()));
        when(inventoryQuery.availableQuantity(productId)).thenReturn(10);

        useCase.place(customerId, new PlaceOrderRequest(addressId, null), null);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        Order order = saved.getValue();
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getSubtotal()).isEqualByComparingTo("200.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("200.00");
        assertThat(order.getShippingAddress()).isNotNull();

        ArgumentCaptor<OrderCreatedEvent> event = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().lines()).hasSize(1);
        assertThat(event.getValue().lines().get(0).quantity()).isEqualTo(2);
        assertThat(event.getValue().couponCode()).isNull();
    }

    @Test
    void appliesCouponDiscount() {
        when(cartQuery.findCart(customerId)).thenReturn(Optional.of(cart()));
        when(inventoryQuery.availableQuantity(productId)).thenReturn(10);
        when(couponQuery.quote("SAVE20", new BigDecimal("200.00")))
                .thenReturn(new CouponQuote("SAVE20", new BigDecimal("20.00")));

        useCase.place(customerId, new PlaceOrderRequest(addressId, "SAVE20"), null);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getDiscountAmount()).isEqualByComparingTo("20.00");
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("180.00");
        assertThat(saved.getValue().getCouponCode()).isEqualTo("SAVE20");
    }

    @Test
    void rejectsEmptyCart() {
        when(cartQuery.findCart(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> useCase.place(customerId, new PlaceOrderRequest(addressId, null), null))
                .isInstanceOf(BusinessException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsCartWithNoItems() {
        when(cartQuery.findCart(customerId))
                .thenReturn(Optional.of(new CartView(List.of(), BigDecimal.ZERO)));

        assertThatThrownBy(
                        () -> useCase.place(customerId, new PlaceOrderRequest(addressId, null), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsUnknownAddress() {
        when(cartQuery.findCart(customerId)).thenReturn(Optional.of(cart()));
        when(userQuery.findAddress(customerId, addressId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> useCase.place(customerId, new PlaceOrderRequest(addressId, null), null))
                .isInstanceOf(EntityNotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsInsufficientStock() {
        when(cartQuery.findCart(customerId)).thenReturn(Optional.of(cart()));
        when(inventoryQuery.availableQuantity(productId)).thenReturn(1);

        assertThatThrownBy(
                        () -> useCase.place(customerId, new PlaceOrderRequest(addressId, null), null))
                .isInstanceOf(BusinessException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void returnsExistingOrderOnIdempotentReplay() {
        Order existing = Order.place("ORD-X", customerId, "USD", null, BigDecimal.ZERO, "key-1");
        when(orderRepository.findByCustomerIdAndIdempotencyKey(customerId, "key-1"))
                .thenReturn(Optional.of(existing));

        useCase.place(customerId, new PlaceOrderRequest(addressId, null), "key-1");

        verify(orderMapper).toResponse(existing);
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
