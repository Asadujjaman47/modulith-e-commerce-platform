package com.company.ecommerce.catalog.application;

import com.company.ecommerce.catalog.api.dto.CreateProductRequest;
import com.company.ecommerce.catalog.api.dto.ProductImageDto;
import com.company.ecommerce.catalog.api.dto.ProductResponse;
import com.company.ecommerce.catalog.domain.Product;
import com.company.ecommerce.catalog.domain.event.ProductCreatedEvent;
import com.company.ecommerce.catalog.infrastructure.mapper.ProductMapper;
import com.company.ecommerce.catalog.infrastructure.persistence.BrandRepository;
import com.company.ecommerce.catalog.infrastructure.persistence.CategoryRepository;
import com.company.ecommerce.catalog.infrastructure.persistence.ProductRepository;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates a product and publishes {@link ProductCreatedEvent}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String slug = Slugs.resolve(request.slug(), request.name());
        if (productRepository.existsBySlug(slug)) {
            throw new BusinessException("Product slug already exists: " + slug);
        }
        if (productRepository.existsBySku(request.sku())) {
            throw new BusinessException("Product SKU already exists: " + request.sku());
        }
        if (!categoryRepository.existsById(request.categoryId())) {
            throw new EntityNotFoundException("Category", request.categoryId());
        }
        if (request.brandId() != null && !brandRepository.existsById(request.brandId())) {
            throw new EntityNotFoundException("Brand", request.brandId());
        }

        Product product =
                Product.create(
                        request.name(),
                        slug,
                        request.sku(),
                        request.description(),
                        request.price(),
                        request.currency().toUpperCase(),
                        request.categoryId(),
                        request.brandId());
        product.replaceImages(toImageData(request.images()));
        product = productRepository.save(product);

        eventPublisher.publishEvent(new ProductCreatedEvent(product.getId(), product.getSku()));
        log.info("Product created. id={} sku={}", product.getId(), product.getSku());
        return productMapper.toResponse(product);
    }

    private List<Product.ImageData> toImageData(List<ProductImageDto> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream()
                .map(i -> new Product.ImageData(i.url(), i.altText(), i.primary()))
                .toList();
    }
}