package com.company.ecommerce.catalog.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.company.ecommerce.catalog.api.dto.CreateCategoryRequest;
import com.company.ecommerce.catalog.api.dto.UpdateCategoryRequest;
import com.company.ecommerce.catalog.domain.Category;
import com.company.ecommerce.catalog.infrastructure.mapper.CategoryMapper;
import com.company.ecommerce.catalog.infrastructure.persistence.CategoryRepository;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.common.exception.EntityNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManageCategoriesUseCaseTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CategoryMapper categoryMapper;
    @InjectMocks private ManageCategoriesUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient()
                .when(categoryRepository.save(any(Category.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void rejectsDuplicateSlugOnCreate() {
        when(categoryRepository.existsBySlug("laptops")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                useCase.create(
                                        new CreateCategoryRequest("Laptops", null, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsMissingParentOnCreate() {
        UUID parentId = UUID.randomUUID();
        when(categoryRepository.existsBySlug("laptops")).thenReturn(false);
        when(categoryRepository.existsById(parentId)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                useCase.create(
                                        new CreateCategoryRequest(
                                                "Laptops", null, null, parentId)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void rejectsSelfParentOnUpdate() {
        UUID id = UUID.randomUUID();
        Category category = Category.create("Laptops", "laptops", null, null);
        when(categoryRepository.findById(id)).thenReturn(Optional.of(category));

        assertThatThrownBy(
                        () ->
                                useCase.update(
                                        id,
                                        new UpdateCategoryRequest(
                                                "Laptops", null, null, id, true)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.get(id)).isInstanceOf(EntityNotFoundException.class);
    }
}