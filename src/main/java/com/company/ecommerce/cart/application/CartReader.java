package com.company.ecommerce.cart.application;

import com.company.ecommerce.cart.domain.Cart;
import com.company.ecommerce.cart.infrastructure.persistence.CartRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Loads (or lazily creates) the single cart belonging to a customer. */
@Component
@RequiredArgsConstructor
class CartReader {

    private final CartRepository cartRepository;

    Cart getOrCreate(UUID customerId) {
        return cartRepository
                .findByCustomerId(customerId)
                .orElseGet(() -> cartRepository.save(Cart.create(customerId)));
    }
}
