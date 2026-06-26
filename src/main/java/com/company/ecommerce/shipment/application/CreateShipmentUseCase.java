package com.company.ecommerce.shipment.application;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.order.spi.OrderQuery;
import com.company.ecommerce.order.spi.OrderView;
import com.company.ecommerce.shipment.api.dto.CreateShipmentRequest;
import com.company.ecommerce.shipment.api.dto.ShipmentResponse;
import com.company.ecommerce.shipment.domain.DeliveryAddress;
import com.company.ecommerce.shipment.domain.Shipment;
import com.company.ecommerce.shipment.domain.event.ShipmentCreatedEvent;
import com.company.ecommerce.shipment.infrastructure.mapper.ShipmentMapper;
import com.company.ecommerce.shipment.infrastructure.persistence.ShipmentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a shipment for a paid order, snapshotting the delivery address from the order {@code spi}
 * and generating a tracking number. One shipment per order: a repeated request returns the existing
 * shipment. Publishes {@link ShipmentCreatedEvent} when a new shipment is created (an in-module
 * listener then advances the order to {@code PROCESSING}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateShipmentUseCase {

    private static final String DEFAULT_CARRIER = "STANDARD";
    private static final Duration DEFAULT_LEAD_TIME = Duration.ofDays(5);

    /** Order statuses from which a shipment may be created (i.e. already paid). */
    private static final Set<String> SHIPPABLE_STATUSES =
            Set.of("PAID", "PROCESSING", "SHIPPED", "DELIVERED");

    private final OrderQuery orderQuery;
    private final ShipmentRepository shipmentRepository;
    private final TrackingNumberGenerator trackingNumberGenerator;
    private final ShipmentMapper shipmentMapper;
    private final ApplicationEventPublisher eventPublisher;

    /** Admin-driven creation: validates the order is paid before shipping. */
    @Transactional
    public ShipmentResponse create(CreateShipmentRequest request) {
        return shipmentMapper.toResponse(createOrGet(request.orderId(), request.carrier(), true));
    }

    /**
     * Event-driven creation (on payment completion). Skips the paid-status check — a completed payment
     * already implies the order is paid, regardless of whether the async status update has propagated.
     */
    @Transactional
    public void createForOrder(UUID orderId) {
        createOrGet(orderId, DEFAULT_CARRIER, false);
    }

    private Shipment createOrGet(UUID orderId, String carrier, boolean validatePaid) {
        Shipment existing = shipmentRepository.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            return existing;
        }

        OrderView order =
                orderQuery
                        .findById(orderId)
                        .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        if (validatePaid && !SHIPPABLE_STATUSES.contains(order.status())) {
            throw new BusinessException(
                    "Order %s is not paid and cannot be shipped (status %s)"
                            .formatted(orderId, order.status()));
        }

        DeliveryAddress address =
                new DeliveryAddress(
                        order.addressLabel(),
                        order.addressLine1(),
                        order.addressLine2(),
                        order.addressCity(),
                        order.addressState(),
                        order.addressPostalCode(),
                        order.addressCountry());

        Shipment shipment =
                Shipment.create(
                        orderId,
                        order.customerId(),
                        carrier,
                        trackingNumberGenerator.generate(),
                        address,
                        Instant.now().plus(DEFAULT_LEAD_TIME));
        shipmentRepository.save(shipment);

        eventPublisher.publishEvent(
                new ShipmentCreatedEvent(
                        shipment.getId(),
                        orderId,
                        order.customerId(),
                        shipment.getTrackingNumber(),
                        carrier));
        log.info(
                "Created shipment {} for order {} ({}, {}).",
                shipment.getId(),
                orderId,
                carrier,
                shipment.getTrackingNumber());
        return shipment;
    }
}
