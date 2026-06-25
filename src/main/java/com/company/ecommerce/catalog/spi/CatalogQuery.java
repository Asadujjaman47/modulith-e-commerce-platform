package com.company.ecommerce.catalog.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * Synchronous read API into the catalog. Implemented inside the {@code catalog} module and consumed
 * by modules allowed to depend on catalog (e.g. {@code cart}).
 */
public interface CatalogQuery {

    /** Returns the product with the given id, or empty if no such product exists. */
    Optional<ProductView> findProduct(UUID productId);
}
