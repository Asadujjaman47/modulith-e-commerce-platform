package com.company.ecommerce.order.domain.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Published when a customer places an order.
 *
 * <p>Consumed by {@code inventory} (reserve stock for each line), {@code coupon} (record usage when a
 * {@code couponCode} is present), {@code cart} (clear the customer's cart) and, in later phases,
 * {@code payment}/{@code notification}/{@code audit}.
 */
public record OrderCreatedEvent(
        UUID orderId,
        String orderNumber,
        UUID customerId,
        List<OrderLine> lines,
        String couponCode,
        BigDecimal discountAmount,
        BigDecimal totalAmount) {}
