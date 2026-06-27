package com.company.ecommerce.catalog.application;

import com.company.ecommerce.catalog.api.dto.CategoryResponse;
import com.company.ecommerce.catalog.api.dto.CreateCategoryRequest;
import com.company.ecommerce.catalog.api.dto.UpdateCategoryRequest;
import com.company.ecommerce.catalog.domain.Category;
import com.company.ecommerce.catalog.infrastructure.mapper.CategoryMapper;
import com.company.ecommerce.catalog.infrastructure.persistence.CategoryRepository;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Create, read, update and delete catalog categories. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManageCategoriesUseCase {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    // Cache name "categoryList" — see config.CacheConfig.CATEGORY_LIST. Evicted on any write.
    @Cacheable(cacheNames = "categoryList")
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryMapper.toResponseList(categoryRepository.findAll());
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(UUID categoryId) {
        return categoryMapper.toResponse(require(categoryId));
    }

    @CacheEvict(cacheNames = "categoryList", allEntries = true)
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        String slug = Slugs.resolve(request.slug(), request.name());
        if (categoryRepository.existsBySlug(slug)) {
            throw new BusinessException("Category slug already exists: " + slug);
        }
        validateParent(request.parentCategoryId());
        Category category =
                categoryRepository.save(
                        Category.create(
                                request.name(), slug, request.description(),
                                request.parentCategoryId()));
        log.info("Category created. id={} slug={}", category.getId(), slug);
        return categoryMapper.toResponse(category);
    }

    @CacheEvict(cacheNames = "categoryList", allEntries = true)
    @Transactional
    public CategoryResponse update(UUID categoryId, UpdateCategoryRequest request) {
        Category category = require(categoryId);
        String slug = Slugs.resolve(request.slug(), request.name());
        if (categoryRepository.existsBySlugAndIdNot(slug, categoryId)) {
            throw new BusinessException("Category slug already exists: " + slug);
        }
        if (categoryId.equals(request.parentCategoryId())) {
            throw new BusinessException("A category cannot be its own parent");
        }
        validateParent(request.parentCategoryId());
        category.update(
                request.name(), slug, request.description(), request.parentCategoryId(),
                request.active());
        log.info("Category updated. id={}", categoryId);
        return categoryMapper.toResponse(category);
    }

    @CacheEvict(cacheNames = "categoryList", allEntries = true)
    @Transactional
    public void delete(UUID categoryId) {
        Category category = require(categoryId);
        categoryRepository.delete(category);
        log.info("Category deleted. id={}", categoryId);
    }

    private void validateParent(UUID parentCategoryId) {
        if (parentCategoryId != null && !categoryRepository.existsById(parentCategoryId)) {
            throw new EntityNotFoundException("Category", parentCategoryId);
        }
    }

    private Category require(UUID categoryId) {
        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category", categoryId));
    }
}