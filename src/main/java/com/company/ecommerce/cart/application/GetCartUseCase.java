package com.company.ecommerce.cart.application;

import com.company.ecommerce.cart.api.dto.CartResponse;
import com.company.ecommerce.cart.infrastructure.mapper.CartMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Returns the authenticated customer's cart, creating an empty one on first access. */
@Service
@RequiredArgsConstructor
public class GetCartUseCase {

    private final CartReader cartReader;
    private final CartMapper cartMapper;

    @Transactional
    public CartResponse getCart(UUID customerId) {
        return cartMapper.toResponse(cartReader.getOrCreate(customerId));
    }
}
