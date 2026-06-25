package com.company.ecommerce.catalog.api;

import com.company.ecommerce.catalog.application.GetProductUseCase;
import com.company.ecommerce.catalog.application.ProductSearchCriteria;
import com.company.ecommerce.catalog.application.SearchProductUseCase;
import com.company.ecommerce.catalog.api.dto.ProductResponse;
import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public (authenticated) product browsing and search endpoints. */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog browsing and search")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final SearchProductUseCase searchProductUseCase;
    private final GetProductUseCase getProductUseCase;

    @GetMapping
    @Operation(
            summary = "List products",
            description =
                    "Returns a paginated, sortable list of products. Supports filtering by"
                            + " category, brand and price range.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Products returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token")
    })
    public ApiResponse<PageResponse<ProductResponse>> list(
            @Parameter(description = "Filter by category id") @RequestParam(required = false)
                    UUID categoryId,
            @Parameter(description = "Filter by brand id") @RequestParam(required = false)
                    UUID brandId,
            @Parameter(description = "Inclusive minimum price") @RequestParam(required = false)
                    BigDecimal minPrice,
            @Parameter(description = "Inclusive maximum price") @RequestParam(required = false)
                    BigDecimal maxPrice,
            @ParameterObject Pageable pageable) {
        ProductSearchCriteria criteria =
                new ProductSearchCriteria(null, categoryId, brandId, minPrice, maxPrice, true);
        return ApiResponse.success(searchProductUseCase.search(criteria, pageable));
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search products",
            description =
                    "Full-text keyword search over product name, description and SKU, with the"
                            + " same filters and pagination as the list endpoint.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Search results returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token")
    })
    public ApiResponse<PageResponse<ProductResponse>> search(
            @Parameter(description = "Free-text keyword") @RequestParam(required = false)
                    String keyword,
            @Parameter(description = "Filter by category id") @RequestParam(required = false)
                    UUID categoryId,
            @Parameter(description = "Filter by brand id") @RequestParam(required = false)
                    UUID brandId,
            @Parameter(description = "Inclusive minimum price") @RequestParam(required = false)
                    BigDecimal minPrice,
            @Parameter(description = "Inclusive maximum price") @RequestParam(required = false)
                    BigDecimal maxPrice,
            @ParameterObject Pageable pageable) {
        ProductSearchCriteria criteria =
                new ProductSearchCriteria(keyword, categoryId, brandId, minPrice, maxPrice, true);
        return ApiResponse.success(searchProductUseCase.search(criteria, pageable));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get a product by id")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Product returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Missing or invalid access token"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Product not found")
    })
    public ApiResponse<ProductResponse> get(@PathVariable UUID productId) {
        return ApiResponse.success(getProductUseCase.getById(productId));
    }
}