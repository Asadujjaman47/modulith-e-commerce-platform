package com.company.ecommerce.order.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The shipping address an {@link Order} is delivered to. Child of the Order aggregate.
 *
 * <p>Snapshots the customer's chosen address (read from the {@code user} module by value at
 * placement time) so the order's delivery destination is stable even if the customer later edits or
 * deletes that address.
 */
@Entity
@Table(name = "order_addresses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderAddress extends AuditableEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "label")
    private String label;

    @Column(name = "line1", nullable = false)
    private String line1;

    @Column(name = "line2")
    private String line2;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(name = "country", nullable = false)
    private String country;

    OrderAddress(
            Order order,
            String label,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country) {
        this.order = order;
        this.label = label;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }
}
