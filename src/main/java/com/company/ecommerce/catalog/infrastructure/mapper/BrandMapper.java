package com.company.ecommerce.catalog.infrastructure.mapper;

import com.company.ecommerce.catalog.api.dto.BrandResponse;
import com.company.ecommerce.catalog.domain.Brand;
import java.util.List;
import org.mapstruct.Mapper;

/** Maps {@link Brand} aggregates to {@link BrandResponse} DTOs. */
@Mapper(componentModel = "spring")
public interface BrandMapper {

    BrandResponse toResponse(Brand brand);

    List<BrandResponse> toResponseList(List<Brand> brands);
}