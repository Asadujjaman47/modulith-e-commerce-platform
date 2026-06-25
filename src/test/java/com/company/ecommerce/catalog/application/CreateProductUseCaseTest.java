package com.company.ecommerce.catalog.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.catalog.api.dto.CreateProductRequest;
import com.company.ecommerce.catalog.domain.Product;
import com.company.ecommerce.catalog.domain.event.ProductCreatedEvent;
import com.company.ecommerce.catalog.infrastructure.mapper.ProductMapper;
import com.company.ecommerce.catalog.infrastructure.persistence.BrandRepository;
import com.company.ecommerce.catalog.infrastructure.persistence.CategoryRepository;
import com.company.ecommerce.catalog.infrastructure.persistence.ProductRepository;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CreateProductUseCaseTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private ProductMapper productMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private CreateProductUseCase useCase;

    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(categoryRepository.existsById(categoryId)).thenReturn(true);
    }

    private CreateProductRequest request(UUID brandId) {
        return new CreateProductRequest(
                "UltraBook 14",
                null,
                "UB-14",
                "A laptop",
                new BigDecimal("1299.00"),
                "usd",
                categoryId,
                brandId,
                List.of());
    }

    @Test
    void createsProductAndPublishesEvent() {
        useCase.create(request(null));

        verify(productRepository).save(any(Product.class));
        verify(eventPublisher).publishEvent(any(ProductCreatedEvent.class));
    }

    @Test
    void rejectsDuplicateSlug() {
        when(productRepository.existsBySlug("ultrabook-14")).thenReturn(true);

        assertThatThrownBy(() -> useCase.create(request(null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsDuplicateSku() {
        when(productRepository.existsBySku("UB-14")).thenReturn(true);

        assertThatThrownBy(() -> useCase.create(request(null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsMissingCategory() {
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.create(request(null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void rejectsMissingBrand() {
        UUID brandId = UUID.randomUUID();
        when(brandRepository.existsById(brandId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.create(request(brandId)))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
