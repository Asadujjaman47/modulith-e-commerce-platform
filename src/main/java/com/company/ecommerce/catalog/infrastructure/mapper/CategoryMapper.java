package com.company.ecommerce.catalog.infrastructure.mapper;

import com.company.ecommerce.catalog.api.dto.CategoryResponse;
import com.company.ecommerce.catalog.domain.Category;
import java.util.List;
import org.mapstruct.Mapper;

/** Maps {@link Category} aggregates to {@link CategoryResponse} DTOs. */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponseList(List<Category> categories);
}