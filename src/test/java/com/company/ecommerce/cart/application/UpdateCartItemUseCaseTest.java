package com.company.ecommerce.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.company.ecommerce.cart.api.dto.UpdateCartItemRequest;
import com.company.ecommerce.cart.domain.Cart;
import com.company.ecommerce.cart.domain.CartItem;
import com.company.ecommerce.cart.infrastructure.mapper.CartMapper;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.inventory.spi.InventoryQuery;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateCartItemUseCaseTest {

    @Mock private CartReader cartReader;
    @Mock private InventoryQuery inventoryQuery;
    @Mock private CartMapper cartMapper;
    @InjectMocks private UpdateCartItemUseCase useCase;

    private final UUID customerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private Cart cart;
    private CartItem item;

    @BeforeEach
    void setUp() {
        cart = Cart.create(customerId);
        item = cart.addItem(productId, "UltraBook", new BigDecimal("1000.00"), 1);
        lenient().when(cartReader.getOrCreate(customerId)).thenReturn(cart);
    }

    @Test
    void updatesQuantityWhenStockAvailable() {
        when(inventoryQuery.availableQuantity(productId)).thenReturn(10);

        useCase.update(customerId, item.getId(), new UpdateCartItemRequest(4));

        assertThat(item.getQuantity()).isEqualTo(4);
    }

    @Test
    void throwsWhenInsufficientStock() {
        when(inventoryQuery.availableQuantity(productId)).thenReturn(3);

        assertThatThrownBy(
                        () -> useCase.update(customerId, item.getId(), new UpdateCartItemRequest(5)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void throwsWhenItemMissing() {
        assertThatThrownBy(
                        () ->
                                useCase.update(
                                        customerId, UUID.randomUUID(), new UpdateCartItemRequest(1)))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
