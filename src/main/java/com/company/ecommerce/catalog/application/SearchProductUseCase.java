package com.company.ecommerce.catalog.application;

import com.company.ecommerce.catalog.api.dto.ProductResponse;
import com.company.ecommerce.catalog.infrastructure.mapper.ProductMapper;
import com.company.ecommerce.catalog.infrastructure.persistence.ProductRepository;
import com.company.ecommerce.catalog.infrastructure.persistence.ProductSpecifications;
import com.company.ecommerce.common.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Browses/searches products with dynamic filtering, pagination and sorting. */
@Service
@RequiredArgsConstructor
public class SearchProductUseCase {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(ProductSearchCriteria criteria, Pageable pageable) {
        return PageResponse.from(
                productRepository
                        .findAll(ProductSpecifications.matching(criteria), pageable)
                        .map(productMapper::toResponse));
    }
}