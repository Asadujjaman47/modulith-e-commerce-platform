package com.company.ecommerce.cart.application;

import com.company.ecommerce.cart.infrastructure.persistence.CartRepository;
import com.company.ecommerce.cart.spi.CartMaintenance;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Default {@link CartMaintenance} implementation backed by the cart repository. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartMaintenanceService implements CartMaintenance {

    private final CartRepository cartRepository;

    @Override
    @Transactional
    public void clear(UUID customerId) {
        cartRepository
                .findByCustomerId(customerId)
                .ifPresent(
                        cart -> {
                            cart.clear();
                            cartRepository.save(cart);
                            log.info("Cart cleared. customerId={}", customerId);
                        });
    }
}
