package com.company.ecommerce.cart.application;

import com.company.ecommerce.cart.api.dto.CartResponse;
import com.company.ecommerce.cart.domain.Cart;
import com.company.ecommerce.cart.infrastructure.mapper.CartMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Removes a line item from the customer's cart. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoveCartItemUseCase {

    private final CartReader cartReader;
    private final CartMapper cartMapper;

    @Transactional
    public CartResponse remove(UUID customerId, UUID itemId) {
        Cart cart = cartReader.getOrCreate(customerId);
        cart.removeItem(itemId);
        log.info("Cart item removed. customerId={} itemId={}", customerId, itemId);
        return cartMapper.toResponse(cart);
    }
}
