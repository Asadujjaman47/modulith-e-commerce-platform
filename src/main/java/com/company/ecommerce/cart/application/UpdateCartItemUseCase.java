package com.company.ecommerce.cart.application;

import com.company.ecommerce.cart.api.dto.CartResponse;
import com.company.ecommerce.cart.api.dto.UpdateCartItemRequest;
import com.company.ecommerce.cart.domain.Cart;
import com.company.ecommerce.cart.domain.CartItem;
import com.company.ecommerce.cart.infrastructure.mapper.CartMapper;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.inventory.spi.InventoryQuery;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sets the quantity of an existing cart item, re-checking stock availability. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateCartItemUseCase {

    private final CartReader cartReader;
    private final InventoryQuery inventoryQuery;
    private final CartMapper cartMapper;

    @Transactional
    public CartResponse update(UUID customerId, UUID itemId, UpdateCartItemRequest request) {
        Cart cart = cartReader.getOrCreate(customerId);
        CartItem item =
                cart.getItems().stream()
                        .filter(i -> i.getId().equals(itemId))
                        .findFirst()
                        .orElseThrow(() -> new EntityNotFoundException("CartItem", itemId));

        int available = inventoryQuery.availableQuantity(item.getProductId());
        if (request.quantity() > available) {
            throw new BusinessException(
                    "Insufficient stock for product %s: requested %d, available %d"
                            .formatted(item.getProductId(), request.quantity(), available));
        }

        cart.updateItemQuantity(itemId, request.quantity());
        log.info(
                "Cart item quantity updated. customerId={} itemId={} quantity={}",
                customerId,
                itemId,
                request.quantity());
        return cartMapper.toResponse(cart);
    }
}
