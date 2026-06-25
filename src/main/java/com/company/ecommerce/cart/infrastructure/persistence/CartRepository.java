package com.company.ecommerce.cart.infrastructure.persistence;

import com.company.ecommerce.cart.domain.Cart;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Cart} aggregates. */
public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByCustomerId(UUID customerId);

    /** All carts containing a line item for the given product. */
    List<Cart> findByItems_ProductId(UUID productId);
}
