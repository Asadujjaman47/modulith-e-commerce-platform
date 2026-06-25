package com.company.ecommerce.catalog.api;

import com.company.ecommerce.catalog.api.dto.BrandResponse;
import com.company.ecommerce.catalog.api.dto.CreateBrandRequest;
import com.company.ecommerce.catalog.api.dto.UpdateBrandRequest;
import com.company.ecommerce.catalog.application.ManageBrandsUseCase;
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

/** Admin brand management. Requires {@code ROLE_ADMIN}. */
@RestController
@RequestMapping("/api/v1/admin/brands")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Brands", description = "Brand management (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminBrandController {

    private final ManageBrandsUseCase manageBrandsUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a brand")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Brand created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Slug already exists")
    })
    public ApiResponse<BrandResponse> create(@Valid @RequestBody CreateBrandRequest request) {
        return ApiResponse.success("Brand created", manageBrandsUseCase.create(request));
    }

    @PutMapping("/{brandId}")
    @Operation(summary = "Update a brand")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Brand updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Brand not found")
    })
    public ApiResponse<BrandResponse> update(
            @PathVariable UUID brandId, @Valid @RequestBody UpdateBrandRequest request) {
        return ApiResponse.success("Brand updated", manageBrandsUseCase.update(brandId, request));
    }

    @DeleteMapping("/{brandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a brand")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "204",
                description = "Brand deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Brand not found")
    })
    public void delete(@PathVariable UUID brandId) {
        manageBrandsUseCase.delete(brandId);
    }
}