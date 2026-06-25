package com.company.ecommerce.catalog.application;

import com.company.ecommerce.catalog.domain.Product;
import com.company.ecommerce.catalog.infrastructure.persistence.ProductRepository;
import com.company.ecommerce.catalog.spi.CatalogQuery;
import com.company.ecommerce.catalog.spi.ProductView;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Default {@link CatalogQuery} implementation backed by the product repository. */
@Service
@RequiredArgsConstructor
public class CatalogQueryService implements CatalogQuery {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductView> findProduct(UUID productId) {
        return productRepository.findById(productId).map(CatalogQueryService::toView);
    }

    private static ProductView toView(Product product) {
        return new ProductView(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice(),
                product.isActive());
    }
}
