package com.company.ecommerce.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.company.ecommerce.cart.api.dto.CartResponse;
import com.company.ecommerce.cart.domain.Cart;
import com.company.ecommerce.cart.infrastructure.mapper.CartMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCartUseCaseTest {

    @Mock private CartReader cartReader;
    @Mock private CartMapper cartMapper;
    @InjectMocks private GetCartUseCase useCase;

    @Test
    void returnsMappedCart() {
        UUID customerId = UUID.randomUUID();
        Cart cart = Cart.create(customerId);
        CartResponse expected =
                new CartResponse(cart.getId(), customerId, List.of(), java.math.BigDecimal.ZERO);
        when(cartReader.getOrCreate(customerId)).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(expected);

        assertThat(useCase.getCart(customerId)).isEqualTo(expected);
    }
}
