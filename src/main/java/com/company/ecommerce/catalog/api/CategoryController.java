package com.company.ecommerce.catalog.api;

import com.company.ecommerce.catalog.api.dto.CategoryResponse;
import com.company.ecommerce.catalog.application.ManageCategoriesUseCase;
import com.company.ecommerce.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public (authenticated) category browsing endpoints. */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product category browsing")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final ManageCategoriesUseCase manageCategoriesUseCase;

    @GetMapping
    @Operation(summary = "List all categories")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Categories returned"))
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.success(manageCategoriesUseCase.list());
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get a category by id")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Category returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Category not found")
    })
    public ApiResponse<CategoryResponse> get(@PathVariable UUID categoryId) {
        return ApiResponse.success(manageCategoriesUseCase.get(categoryId));
    }
}