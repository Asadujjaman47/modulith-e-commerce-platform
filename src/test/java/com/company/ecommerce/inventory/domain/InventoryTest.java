package com.company.ecommerce.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ecommerce.common.exception.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryTest {

    private Inventory inventory(int onHand) {
        return Inventory.create(UUID.randomUUID(), onHand);
    }

    @Test
    void reserveReducesAvailable() {
        Inventory inventory = inventory(10);

        inventory.reserve(3);

        assertThat(inventory.getQuantityReserved()).isEqualTo(3);
        assertThat(inventory.available()).isEqualTo(7);
    }

    @Test
    void reserveRejectsWhenInsufficient() {
        Inventory inventory = inventory(2);

        assertThatThrownBy(() -> inventory.reserve(5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void releaseReturnsStockToAvailable() {
        Inventory inventory = inventory(10);
        inventory.reserve(4);

        inventory.release(4);

        assertThat(inventory.getQuantityReserved()).isZero();
        assertThat(inventory.available()).isEqualTo(10);
    }

    @Test
    void releaseRejectsMoreThanReserved() {
        Inventory inventory = inventory(10);
        inventory.reserve(2);

        assertThatThrownBy(() -> inventory.release(3)).isInstanceOf(BusinessException.class);
    }

    @Test
    void setOnHandRejectsValueBelowReserved() {
        Inventory inventory = inventory(10);
        inventory.reserve(6);

        assertThatThrownBy(() -> inventory.setOnHand(5)).isInstanceOf(BusinessException.class);
    }

    @Test
    void setOnHandUpdatesQuantity() {
        Inventory inventory = inventory(10);

        inventory.setOnHand(25);

        assertThat(inventory.getQuantityOnHand()).isEqualTo(25);
        assertThat(inventory.available()).isEqualTo(25);
    }
}