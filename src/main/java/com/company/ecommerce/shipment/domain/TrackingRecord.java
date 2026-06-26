package com.company.ecommerce.shipment.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An append-only entry in a {@link Shipment}'s tracking history, recorded for every status change.
 * Child of the Shipment aggregate — never referenced or persisted independently of its owning
 * shipment.
 */
@Entity
@Table(name = "tracking_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackingRecord extends AuditableEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ShipmentStatus status;

    @Column(name = "location")
    private String location;

    @Column(name = "note")
    private String note;

    TrackingRecord(Shipment shipment, ShipmentStatus status, String location, String note) {
        this.shipment = shipment;
        this.status = status;
        this.location = location;
        this.note = note;
    }
}
