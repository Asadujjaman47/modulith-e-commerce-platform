package com.company.ecommerce.catalog.infrastructure.mapper;

import com.company.ecommerce.catalog.api.dto.ProductImageDto;
import com.company.ecommerce.catalog.api.dto.ProductResponse;
import com.company.ecommerce.catalog.domain.Product;
import com.company.ecommerce.catalog.domain.ProductImage;
import org.mapstruct.Mapper;

/** Maps {@link Product} aggregates to {@link ProductResponse} DTOs. */
@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    ProductImageDto toImageDto(ProductImage image);
}