package com.company.ecommerce.catalog.application;

import com.company.ecommerce.catalog.domain.event.ProductDeletedEvent;
import com.company.ecommerce.catalog.infrastructure.persistence.ProductRepository;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deletes a product and publishes {@link ProductDeletedEvent}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteProductUseCase {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void delete(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("Product", productId);
        }
        productRepository.deleteById(productId);
        eventPublisher.publishEvent(new ProductDeletedEvent(productId));
        log.info("Product deleted. id={}", productId);
    }
}