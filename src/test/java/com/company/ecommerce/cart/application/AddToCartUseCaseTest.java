package com.company.ecommerce.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.company.ecommerce.cart.api.dto.AddToCartRequest;
import com.company.ecommerce.cart.domain.Cart;
import com.company.ecommerce.cart.infrastructure.mapper.CartMapper;
import com.company.ecommerce.catalog.spi.CatalogQuery;
import com.company.ecommerce.catalog.spi.ProductView;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.inventory.spi.InventoryQuery;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddToCartUseCaseTest {

    @Mock private CartReader cartReader;
    @Mock private CatalogQuery catalogQuery;
    @Mock private InventoryQuery inventoryQuery;
    @Mock private CartMapper cartMapper;
    @InjectMocks private AddToCartUseCase useCase;

    private final UUID customerId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = Cart.create(customerId);
        lenient().when(cartReader.getOrCreate(customerId)).thenReturn(cart);
    }

    private ProductView product(boolean active) {
        return new ProductView(productId, "UltraBook", "UB-1", new BigDecimal("1000.00"), active);
    }

    @Test
    void addsItemWhenProductActiveAndStockAvailable() {
        when(catalogQuery.findProduct(productId)).thenReturn(Optional.of(product(true)));
        when(inventoryQuery.availableQuantity(productId)).thenReturn(10);

        useCase.add(customerId, new AddToCartRequest(productId, 3));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(cart.getItems().get(0).getUnitPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    void throwsWhenProductMissing() {
        when(catalogQuery.findProduct(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.add(customerId, new AddToCartRequest(productId, 1)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void throwsWhenProductInactive() {
        when(catalogQuery.findProduct(productId)).thenReturn(Optional.of(product(false)));

        assertThatThrownBy(() -> useCase.add(customerId, new AddToCartRequest(productId, 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void throwsWhenInsufficientStock() {
        when(catalogQuery.findProduct(productId)).thenReturn(Optional.of(product(true)));
        when(inventoryQuery.availableQuantity(productId)).thenReturn(2);

        assertThatThrownBy(() -> useCase.add(customerId, new AddToCartRequest(productId, 5)))
                .isInstanceOf(BusinessException.class);
        assertThat(cart.getItems()).isEmpty();
    }
}
