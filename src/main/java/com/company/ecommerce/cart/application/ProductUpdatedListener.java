package com.company.ecommerce.cart.application;

import com.company.ecommerce.cart.domain.Cart;
import com.company.ecommerce.cart.infrastructure.persistence.CartRepository;
import com.company.ecommerce.catalog.domain.event.ProductUpdatedEvent;
import com.company.ecommerce.catalog.spi.CatalogQuery;
import com.company.ecommerce.catalog.spi.ProductView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Keeps cart line-item snapshots in step with the catalog. When a product changes, refreshes the
 * cached name and unit price on any cart that contains it. This is the cart module's allowed
 * {@code cart -> catalog} coupling (a consumed event plus the catalog {@code spi}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductUpdatedListener {

    private final CartRepository cartRepository;
    private final CatalogQuery catalogQuery;

    @ApplicationModuleListener
    public void on(ProductUpdatedEvent event) {
        ProductView product = catalogQuery.findProduct(event.productId()).orElse(null);
        if (product == null) {
            return;
        }
        List<Cart> carts = cartRepository.findByItems_ProductId(product.id());
        carts.forEach(cart -> cart.refreshProduct(product.id(), product.name(), product.price()));
        if (!carts.isEmpty()) {
            cartRepository.saveAll(carts);
            log.info(
                    "Refreshed product snapshot in {} cart(s). productId={}",
                    carts.size(),
                    product.id());
        }
    }
}
