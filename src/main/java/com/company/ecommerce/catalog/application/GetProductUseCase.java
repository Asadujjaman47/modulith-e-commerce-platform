package com.company.ecommerce.catalog.application;

import com.company.ecommerce.catalog.api.dto.ProductResponse;
import com.company.ecommerce.catalog.infrastructure.mapper.ProductMapper;
import com.company.ecommerce.catalog.infrastructure.persistence.ProductRepository;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads a single product by id. */
@Service
@RequiredArgsConstructor
public class GetProductUseCase {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID productId) {
        return productRepository
                .findById(productId)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Product", productId));
    }
}