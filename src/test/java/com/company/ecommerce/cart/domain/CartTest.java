package com.company.ecommerce.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartTest {

    private final UUID customerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @Test
    void addsNewItemWithPriceSnapshot() {
        Cart cart = Cart.create(customerId);

        CartItem item = cart.addItem(productId, "UltraBook", new BigDecimal("1000.00"), 2);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(item.lineTotal()).isEqualByComparingTo("2000.00");
        assertThat(cart.subtotal()).isEqualByComparingTo("2000.00");
    }

    @Test
    void mergesQuantityWhenSameProductAddedAgain() {
        Cart cart = Cart.create(customerId);
        cart.addItem(productId, "UltraBook", new BigDecimal("1000.00"), 2);

        cart.addItem(productId, "UltraBook", new BigDecimal("1100.00"), 3);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        // snapshot refreshed to the latest price
        assertThat(cart.getItems().get(0).getUnitPrice()).isEqualByComparingTo("1100.00");
    }

    @Test
    void updatesItemQuantity() {
        Cart cart = Cart.create(customerId);
        CartItem item = cart.addItem(productId, "UltraBook", new BigDecimal("1000.00"), 2);

        cart.updateItemQuantity(item.getId(), 7);

        assertThat(item.getQuantity()).isEqualTo(7);
    }

    @Test
    void rejectsNonPositiveQuantity() {
        Cart cart = Cart.create(customerId);
        CartItem item = cart.addItem(productId, "UltraBook", new BigDecimal("1000.00"), 2);

        assertThatThrownBy(() -> cart.updateItemQuantity(item.getId(), 0))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void removesItem() {
        Cart cart = Cart.create(customerId);
        CartItem item = cart.addItem(productId, "UltraBook", new BigDecimal("1000.00"), 2);

        cart.removeItem(item.getId());

        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.subtotal()).isEqualByComparingTo("0");
    }

    @Test
    void throwsWhenUpdatingMissingItem() {
        Cart cart = Cart.create(customerId);

        assertThatThrownBy(() -> cart.updateItemQuantity(UUID.randomUUID(), 1))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void refreshesProductSnapshot() {
        Cart cart = Cart.create(customerId);
        cart.addItem(productId, "UltraBook", new BigDecimal("1000.00"), 2);

        cart.refreshProduct(productId, "UltraBook Pro", new BigDecimal("1500.00"));

        CartItem item = cart.getItems().get(0);
        assertThat(item.getProductName()).isEqualTo("UltraBook Pro");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("1500.00");
    }
}
