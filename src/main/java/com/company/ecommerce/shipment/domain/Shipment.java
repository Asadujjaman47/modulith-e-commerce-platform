package com.company.ecommerce.shipment.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import com.company.ecommerce.common.exception.BusinessException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Shipment aggregate root. Owned by the {@code shipment} module.
 *
 * <p>Created for a paid order (one shipment per order) with a snapshot of the delivery address read
 * from the {@code order} module by value. Progresses through the {@link ShipmentStatus} state machine,
 * appending a {@link TrackingRecord} for every status change. References the order and customer by id
 * value only — no cross-module FKs.
 */
@Entity
@Table(name = "shipments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipment extends AuditableEntity {

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShipmentStatus status;

    @Column(name = "carrier", nullable = false)
    private String carrier;

    @Column(name = "tracking_number", nullable = false, unique = true)
    private String trackingNumber;

    @Embedded private DeliveryAddress deliveryAddress;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "estimated_delivery")
    private Instant estimatedDelivery;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<TrackingRecord> trackingRecords = new ArrayList<>();

    private Shipment(
            UUID orderId,
            UUID customerId,
            String carrier,
            String trackingNumber,
            DeliveryAddress deliveryAddress,
            Instant estimatedDelivery) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
        this.deliveryAddress = deliveryAddress;
        this.estimatedDelivery = estimatedDelivery;
        this.status = ShipmentStatus.CREATED;
        addTracking(ShipmentStatus.CREATED, null, "Shipment created");
    }

    /** Creates a new {@code CREATED} shipment with an initial tracking record. */
    public static Shipment create(
            UUID orderId,
            UUID customerId,
            String carrier,
            String trackingNumber,
            DeliveryAddress deliveryAddress,
            Instant estimatedDelivery) {
        return new Shipment(
                orderId, customerId, carrier, trackingNumber, deliveryAddress, estimatedDelivery);
    }

    /** Advances the shipment one step to {@code target}, enforcing the allowed-transition rules. */
    public void advanceTo(ShipmentStatus target, String location, String note) {
        if (status == target) {
            throw new BusinessException("Shipment is already in status " + status);
        }
        if (!status.canTransitionTo(target)) {
            throw new BusinessException(
                    "Illegal shipment status transition: %s -> %s".formatted(status, target));
        }
        applyStatus(target, location, note);
    }

    /**
     * Confirms delivery directly (admin "deliver" action). Allowed from any non-delivered status —
     * a deliberate override of the step-by-step flow — and fails only if already delivered.
     */
    public void markDelivered(String location, String note) {
        if (status.isDelivered()) {
            throw new BusinessException("Shipment is already delivered");
        }
        applyStatus(ShipmentStatus.DELIVERED, location, note);
    }

    private void applyStatus(ShipmentStatus target, String location, String note) {
        this.status = target;
        if (target == ShipmentStatus.PICKED_UP && shippedAt == null) {
            this.shippedAt = Instant.now();
        }
        if (target == ShipmentStatus.DELIVERED) {
            this.deliveredAt = Instant.now();
            if (this.shippedAt == null) {
                this.shippedAt = Instant.now();
            }
        }
        addTracking(target, location, note);
    }

    private void addTracking(ShipmentStatus status, String location, String note) {
        trackingRecords.add(new TrackingRecord(this, status, location, note));
    }
}
