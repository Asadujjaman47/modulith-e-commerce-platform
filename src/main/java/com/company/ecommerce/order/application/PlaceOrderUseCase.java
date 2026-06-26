package com.company.ecommerce.order.application;

import com.company.ecommerce.cart.spi.CartLineView;
import com.company.ecommerce.cart.spi.CartQuery;
import com.company.ecommerce.cart.spi.CartView;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.coupon.spi.CouponQuery;
import com.company.ecommerce.coupon.spi.CouponQuote;
import com.company.ecommerce.inventory.spi.InventoryQuery;
import com.company.ecommerce.order.api.dto.OrderResponse;
import com.company.ecommerce.order.api.dto.PlaceOrderRequest;
import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.order.domain.event.OrderLine;
import com.company.ecommerce.order.infrastructure.mapper.OrderMapper;
import com.company.ecommerce.order.infrastructure.persistence.OrderRepository;
import com.company.ecommerce.user.spi.AddressView;
import com.company.ecommerce.user.spi.UserQuery;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Places an order from a snapshot of the customer's cart.
 *
 * <p>Reads the cart, shipping address and (optional) coupon discount synchronously through their
 * {@code spi} named interfaces and pre-checks stock availability for a fast {@code 409}. The order is
 * persisted as {@code PENDING} and an {@link OrderCreatedEvent} is published; stock reservation,
 * coupon-usage recording and cart clearing happen asynchronously in the respective modules so each
 * runs in its own transaction. An {@code Idempotency-Key} makes the operation safe to retry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceOrderUseCase {

    private static final String DEFAULT_CURRENCY = "USD";

    private final OrderRepository orderRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final CartQuery cartQuery;
    private final UserQuery userQuery;
    private final CouponQuery couponQuery;
    private final InventoryQuery inventoryQuery;
    private final OrderMapper orderMapper;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponse place(UUID customerId, PlaceOrderRequest request, String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            var existing =
                    orderRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey);
            if (existing.isPresent()) {
                log.info(
                        "Idempotent replay of place-order. customerId={} key={} orderId={}",
                        customerId,
                        idempotencyKey,
                        existing.get().getId());
                return orderMapper.toResponse(existing.get());
            }
        }

        CartView cart =
                cartQuery
                        .findCart(customerId)
                        .filter(view -> !view.isEmpty())
                        .orElseThrow(
                                () -> new BusinessException("Cannot place an order with an empty cart"));

        AddressView address =
                userQuery
                        .findAddress(customerId, request.addressId())
                        .orElseThrow(
                                () -> new EntityNotFoundException("Address", request.addressId()));

        cart.items().forEach(this::ensureStockAvailable);

        String couponCode = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (StringUtils.hasText(request.couponCode())) {
            CouponQuote quote = couponQuery.quote(request.couponCode(), cart.subtotal());
            couponCode = quote.code();
            discount = quote.discountAmount();
        }

        Order order =
                Order.place(
                        orderNumberGenerator.generate(),
                        customerId,
                        DEFAULT_CURRENCY,
                        couponCode,
                        discount,
                        StringUtils.hasText(idempotencyKey) ? idempotencyKey : null);
        cart.items()
                .forEach(
                        line ->
                                order.addItem(
                                        line.productId(),
                                        line.productName(),
                                        line.unitPrice(),
                                        line.quantity()));
        order.setShippingAddress(
                address.label(),
                address.line1(),
                address.line2(),
                address.city(),
                address.state(),
                address.postalCode(),
                address.country());
        order.recalculateTotals();
        orderRepository.save(order);

        eventPublisher.publishEvent(
                new OrderCreatedEvent(
                        order.getId(),
                        order.getOrderNumber(),
                        customerId,
                        order.getItems().stream()
                                .map(item -> new OrderLine(item.getProductId(), item.getQuantity()))
                                .toList(),
                        order.getCouponCode(),
                        order.getDiscountAmount(),
                        order.getTotalAmount()));
        log.info(
                "Order placed. orderId={} orderNumber={} customerId={} total={}",
                order.getId(),
                order.getOrderNumber(),
                customerId,
                order.getTotalAmount());
        return orderMapper.toResponse(order);
    }

    private void ensureStockAvailable(CartLineView line) {
        int available = inventoryQuery.availableQuantity(line.productId());
        if (line.quantity() > available) {
            throw new BusinessException(
                    "Insufficient stock for product %s: requested %d, available %d"
                            .formatted(line.productId(), line.quantity(), available));
        }
    }
}
