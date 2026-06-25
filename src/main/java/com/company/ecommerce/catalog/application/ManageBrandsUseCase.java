package com.company.ecommerce.catalog.application;

import com.company.ecommerce.catalog.api.dto.BrandResponse;
import com.company.ecommerce.catalog.api.dto.CreateBrandRequest;
import com.company.ecommerce.catalog.api.dto.UpdateBrandRequest;
import com.company.ecommerce.catalog.domain.Brand;
import com.company.ecommerce.catalog.infrastructure.mapper.BrandMapper;
import com.company.ecommerce.catalog.infrastructure.persistence.BrandRepository;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Create, read, update and delete catalog brands. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManageBrandsUseCase {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Transactional(readOnly = true)
    public List<BrandResponse> list() {
        return brandMapper.toResponseList(brandRepository.findAll());
    }

    @Transactional(readOnly = true)
    public BrandResponse get(UUID brandId) {
        return brandMapper.toResponse(require(brandId));
    }

    @Transactional
    public BrandResponse create(CreateBrandRequest request) {
        String slug = Slugs.resolve(request.slug(), request.name());
        if (brandRepository.existsBySlug(slug)) {
            throw new BusinessException("Brand slug already exists: " + slug);
        }
        Brand brand =
                brandRepository.save(
                        Brand.create(
                                request.name(), slug, request.description(), request.logoUrl()));
        log.info("Brand created. id={} slug={}", brand.getId(), slug);
        return brandMapper.toResponse(brand);
    }

    @Transactional
    public BrandResponse update(UUID brandId, UpdateBrandRequest request) {
        Brand brand = require(brandId);
        String slug = Slugs.resolve(request.slug(), request.name());
        if (brandRepository.existsBySlugAndIdNot(slug, brandId)) {
            throw new BusinessException("Brand slug already exists: " + slug);
        }
        brand.update(
                request.name(), slug, request.description(), request.logoUrl(), request.active());
        log.info("Brand updated. id={}", brandId);
        return brandMapper.toResponse(brand);
    }

    @Transactional
    public void delete(UUID brandId) {
        Brand brand = require(brandId);
        brandRepository.delete(brand);
        log.info("Brand deleted. id={}", brandId);
    }

    private Brand require(UUID brandId) {
        return brandRepository
                .findById(brandId)
                .orElseThrow(() -> new EntityNotFoundException("Brand", brandId));
    }
}