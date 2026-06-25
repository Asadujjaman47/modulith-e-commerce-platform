package com.company.ecommerce.catalog.application;

import com.company.ecommerce.catalog.api.dto.ProductImageDto;
import com.company.ecommerce.catalog.api.dto.ProductResponse;
import com.company.ecommerce.catalog.api.dto.UpdateProductRequest;
import com.company.ecommerce.catalog.domain.Product;
import com.company.ecommerce.catalog.domain.event.ProductUpdatedEvent;
import com.company.ecommerce.catalog.infrastructure.mapper.ProductMapper;
import com.company.ecommerce.catalog.infrastructure.persistence.BrandRepository;
import com.company.ecommerce.catalog.infrastructure.persistence.CategoryRepository;
import com.company.ecommerce.catalog.infrastructure.persistence.ProductRepository;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Updates a product and publishes {@link ProductUpdatedEvent}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProductResponse update(UUID productId, UpdateProductRequest request) {
        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() -> new EntityNotFoundException("Product", productId));

        String slug = Slugs.resolve(request.slug(), request.name());
        if (productRepository.existsBySlugAndIdNot(slug, productId)) {
            throw new BusinessException("Product slug already exists: " + slug);
        }
        if (!categoryRepository.existsById(request.categoryId())) {
            throw new EntityNotFoundException("Category", request.categoryId());
        }
        if (request.brandId() != null && !brandRepository.existsById(request.brandId())) {
            throw new EntityNotFoundException("Brand", request.brandId());
        }

        product.update(
                request.name(),
                slug,
                request.description(),
                request.price(),
                request.currency().toUpperCase(),
                request.categoryId(),
                request.brandId(),
                request.active());
        if (request.images() != null) {
            product.replaceImages(toImageData(request.images()));
        }

        eventPublisher.publishEvent(new ProductUpdatedEvent(productId));
        log.info("Product updated. id={}", productId);
        return productMapper.toResponse(product);
    }

    private List<Product.ImageData> toImageData(List<ProductImageDto> images) {
        return images.stream()
                .map(i -> new Product.ImageData(i.url(), i.altText(), i.primary()))
                .toList();
    }
}