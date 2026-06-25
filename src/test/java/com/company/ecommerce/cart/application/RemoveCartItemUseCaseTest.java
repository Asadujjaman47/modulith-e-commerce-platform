package com.company.ecommerce.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.company.ecommerce.cart.domain.Cart;
import com.company.ecommerce.cart.domain.CartItem;
import com.company.ecommerce.cart.infrastructure.mapper.CartMapper;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoveCartItemUseCaseTest {

    @Mock private CartReader cartReader;
    @Mock private CartMapper cartMapper;
    @InjectMocks private RemoveCartItemUseCase useCase;

    private final UUID customerId = UUID.randomUUID();

    @Test
    void removesItem() {
        Cart cart = Cart.create(customerId);
        CartItem item = cart.addItem(UUID.randomUUID(), "UltraBook", new BigDecimal("10.00"), 1);
        when(cartReader.getOrCreate(customerId)).thenReturn(cart);

        useCase.remove(customerId, item.getId());

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void throwsWhenItemMissing() {
        Cart cart = Cart.create(customerId);
        when(cartReader.getOrCreate(customerId)).thenReturn(cart);

        assertThatThrownBy(() -> useCase.remove(customerId, UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
