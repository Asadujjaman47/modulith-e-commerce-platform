package com.company.ecommerce.cart.application;

import com.company.ecommerce.cart.domain.Cart;
import com.company.ecommerce.cart.infrastructure.persistence.CartRepository;
import com.company.ecommerce.cart.spi.CartLineView;
import com.company.ecommerce.cart.spi.CartQuery;
import com.company.ecommerce.cart.spi.CartView;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Default {@link CartQuery} implementation backed by the cart repository. */
@Service
@RequiredArgsConstructor
public class CartQueryService implements CartQuery {

    private final CartRepository cartRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<CartView> findCart(UUID customerId) {
        return cartRepository.findByCustomerId(customerId).map(CartQueryService::toView);
    }

    private static CartView toView(Cart cart) {
        return new CartView(
                cart.getItems().stream()
                        .map(
                                item ->
                                        new CartLineView(
                                                item.getProductId(),
                                                item.getProductName(),
                                                item.getUnitPrice(),
                                                item.getQuantity()))
                        .toList(),
                cart.subtotal());
    }
}
