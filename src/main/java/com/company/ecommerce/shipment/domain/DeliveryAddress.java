package com.company.ecommerce.shipment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The address a {@link Shipment} is delivered to, snapshotted from the order at creation time so the
 * destination is stable even if the underlying order/customer address later changes. Value object
 * embedded in the shipment row.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAddress {

    @Column(name = "address_label")
    private String label;

    @Column(name = "address_line1", nullable = false)
    private String line1;

    @Column(name = "address_line2")
    private String line2;

    @Column(name = "address_city", nullable = false)
    private String city;

    @Column(name = "address_state")
    private String state;

    @Column(name = "address_postal_code", nullable = false)
    private String postalCode;

    @Column(name = "address_country", nullable = false)
    private String country;

    public DeliveryAddress(
            String label,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country) {
        this.label = label;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }
}
