package com.company.ecommerce.catalog.api;

import com.company.ecommerce.catalog.api.dto.CategoryResponse;
import com.company.ecommerce.catalog.api.dto.CreateCategoryRequest;
import com.company.ecommerce.catalog.api.dto.UpdateCategoryRequest;
import com.company.ecommerce.catalog.application.ManageCategoriesUseCase;
import com.company.ecommerce.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin category management. Requires {@code ROLE_ADMIN}. */
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Categories", description = "Category management (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminCategoryController {

    private final ManageCategoriesUseCase manageCategoriesUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Category created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Slug already exists")
    })
    public ApiResponse<CategoryResponse> create(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success("Category created", manageCategoriesUseCase.create(request));
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update a category")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Category updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Category not found")
    })
    public ApiResponse<CategoryResponse> update(
            @PathVariable UUID categoryId, @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.success(
                "Category updated", manageCategoriesUseCase.update(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a category")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "204",
                description = "Category deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Category not found")
    })
    public void delete(@PathVariable UUID categoryId) {
        manageCategoriesUseCase.delete(categoryId);
    }
}