package com.company.ecommerce.catalog.api;

import com.company.ecommerce.catalog.api.dto.BrandResponse;
import com.company.ecommerce.catalog.application.ManageBrandsUseCase;
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

/** Public (authenticated) brand browsing endpoints. */
@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
@Tag(name = "Brands", description = "Product brand browsing")
@SecurityRequirement(name = "bearerAuth")
public class BrandController {

    private final ManageBrandsUseCase manageBrandsUseCase;

    @GetMapping
    @Operation(summary = "List all brands")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Brands returned"))
    public ApiResponse<List<BrandResponse>> list() {
        return ApiResponse.success(manageBrandsUseCase.list());
    }

    @GetMapping("/{brandId}")
    @Operation(summary = "Get a brand by id")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Brand returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Brand not found")
    })
    public ApiResponse<BrandResponse> get(@PathVariable UUID brandId) {
        return ApiResponse.success(manageBrandsUseCase.get(brandId));
    }
}