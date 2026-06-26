package com.company.ecommerce.shipment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ecommerce.common.exception.BusinessException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShipmentTest {

    private Shipment shipment() {
        DeliveryAddress address =
                new DeliveryAddress("Home", "221B Baker St", null, "London", null, "NW1 6XE", "GB");
        return Shipment.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "DHL",
                "TRK-1",
                address,
                Instant.now().plus(5, ChronoUnit.DAYS));
    }

    @Test
    void createsInCreatedStatusWithInitialTracking() {
        Shipment shipment = shipment();

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CREATED);
        assertThat(shipment.getTrackingNumber()).isEqualTo("TRK-1");
        assertThat(shipment.getTrackingRecords()).hasSize(1);
        assertThat(shipment.getTrackingRecords().get(0).getStatus()).isEqualTo(ShipmentStatus.CREATED);
    }

    @Test
    void advancesAndStampsShippedAtOnPickup() {
        Shipment shipment = shipment();

        shipment.advanceTo(ShipmentStatus.PICKED_UP, "Depot", "Collected");

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.PICKED_UP);
        assertThat(shipment.getShippedAt()).isNotNull();
        assertThat(shipment.getTrackingRecords()).hasSize(2);
    }

    @Test
    void rejectsIllegalTransition() {
        Shipment shipment = shipment();

        assertThatThrownBy(() -> shipment.advanceTo(ShipmentStatus.IN_TRANSIT, null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsTransitionToSameStatus() {
        Shipment shipment = shipment();

        assertThatThrownBy(() -> shipment.advanceTo(ShipmentStatus.CREATED, null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void marksDeliveredFromInTransit() {
        Shipment shipment = shipment();
        shipment.advanceTo(ShipmentStatus.PICKED_UP, null, null);
        shipment.advanceTo(ShipmentStatus.IN_TRANSIT, null, null);

        shipment.markDelivered("Doorstep", "Left with neighbour");

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(shipment.getDeliveredAt()).isNotNull();
    }

    @Test
    void markDeliveredOverridesFromCreated() {
        Shipment shipment = shipment();

        shipment.markDelivered(null, null);

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(shipment.getDeliveredAt()).isNotNull();
        assertThat(shipment.getShippedAt()).isNotNull();
    }

    @Test
    void cannotDeliverTwice() {
        Shipment shipment = shipment();
        shipment.markDelivered(null, null);

        assertThatThrownBy(() -> shipment.markDelivered(null, null))
                .isInstanceOf(BusinessException.class);
    }
}
