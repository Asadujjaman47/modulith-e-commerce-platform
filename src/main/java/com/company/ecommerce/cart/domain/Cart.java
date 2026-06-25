package com.company.ecommerce.cart.domain;

import com.company.ecommerce.common.domain.AuditableEntity;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Shopping cart aggregate root. Owned by the {@code cart} module.
 *
 * <p>One active cart per customer (referenced by {@code customerId} value). {@link CartItem}s are
 * children of this aggregate and managed exclusively through it.
 */
@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart extends AuditableEntity {

    @Column(name = "customer_id", nullable = false, unique = true)
    private UUID customerId;

    @OneToMany(
            mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    private Cart(UUID customerId) {
        this.customerId = customerId;
    }

    public static Cart create(UUID customerId) {
        return new Cart(customerId);
    }

    /**
     * Adds a product to the cart. If the product is already present, its quantity is increased;
     * otherwise a new line is created with the supplied price/name snapshot.
     */
    public CartItem addItem(UUID productId, String productName, BigDecimal unitPrice, int quantity) {
        return findItemByProduct(productId)
                .map(
                        existing -> {
                            existing.increaseQuantity(quantity);
                            existing.refreshProduct(productName, unitPrice);
                            return existing;
                        })
                .orElseGet(
                        () -> {
                            CartItem item =
                                    new CartItem(this, productId, productName, unitPrice, quantity);
                            items.add(item);
                            return item;
                        });
    }

    /** Sets the absolute quantity of an existing item, identified by item id. */
    public CartItem updateItemQuantity(UUID itemId, int quantity) {
        CartItem item = requireItem(itemId);
        item.setQuantity(quantity);
        return item;
    }

    /** Removes an item from the cart by item id. */
    public void removeItem(UUID itemId) {
        CartItem item = requireItem(itemId);
        items.remove(item);
    }

    /** Refreshes the price/name snapshot of any line referencing the given product. */
    public void refreshProduct(UUID productId, String productName, BigDecimal unitPrice) {
        findItemByProduct(productId)
                .ifPresent(item -> item.refreshProduct(productName, unitPrice));
    }

    /** Sum of all line totals. */
    public BigDecimal subtotal() {
        return items.stream()
                .map(CartItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<CartItem> findItemByProduct(UUID productId) {
        return items.stream().filter(item -> item.getProductId().equals(productId)).findFirst();
    }

    private CartItem requireItem(UUID itemId) {
        return items.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("CartItem", itemId));
    }
}
