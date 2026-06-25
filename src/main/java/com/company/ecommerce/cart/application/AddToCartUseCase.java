package com.company.ecommerce.cart.application;

import com.company.ecommerce.cart.api.dto.AddToCartRequest;
import com.company.ecommerce.cart.api.dto.CartResponse;
import com.company.ecommerce.cart.domain.Cart;
import com.company.ecommerce.cart.domain.CartItem;
import com.company.ecommerce.cart.infrastructure.mapper.CartMapper;
import com.company.ecommerce.catalog.spi.CatalogQuery;
import com.company.ecommerce.catalog.spi.ProductView;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.inventory.spi.InventoryQuery;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds a product to the customer's cart, snapshotting its current catalog price and verifying that
 * enough stock is available for the resulting quantity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AddToCartUseCase {

    private final CartReader cartReader;
    private final CatalogQuery catalogQuery;
    private final InventoryQuery inventoryQuery;
    private final CartMapper cartMapper;

    @Transactional
    public CartResponse add(UUID customerId, AddToCartRequest request) {
        ProductView product =
                catalogQuery
                        .findProduct(request.productId())
                        .orElseThrow(
                                () -> new EntityNotFoundException("Product", request.productId()));
        if (!product.active()) {
            throw new BusinessException("Product is not available: " + product.id());
        }

        Cart cart = cartReader.getOrCreate(customerId);
        int existingQuantity =
                cart.getItems().stream()
                        .filter(item -> item.getProductId().equals(product.id()))
                        .mapToInt(CartItem::getQuantity)
                        .findFirst()
                        .orElse(0);
        int desiredQuantity = existingQuantity + request.quantity();
        ensureStockAvailable(product.id(), desiredQuantity);

        cart.addItem(product.id(), product.name(), product.price(), request.quantity());
        log.info(
                "Item added to cart. customerId={} productId={} quantity={}",
                customerId,
                product.id(),
                request.quantity());
        return cartMapper.toResponse(cart);
    }

    private void ensureStockAvailable(UUID productId, int requestedQuantity) {
        int available = inventoryQuery.availableQuantity(productId);
        if (requestedQuantity > available) {
            throw new BusinessException(
                    "Insufficient stock for product %s: requested %d, available %d"
                            .formatted(productId, requestedQuantity, available));
        }
    }
}
