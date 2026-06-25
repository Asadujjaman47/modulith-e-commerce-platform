package com.company.ecommerce.inventory.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.inventory.api.dto.InventoryResponse;
import com.company.ecommerce.inventory.api.dto.ReleaseStockRequest;
import com.company.ecommerce.inventory.api.dto.ReservationResponse;
import com.company.ecommerce.inventory.api.dto.ReserveStockRequest;
import com.company.ecommerce.inventory.api.dto.UpdateStockRequest;
import com.company.ecommerce.inventory.application.GetStockUseCase;
import com.company.ecommerce.inventory.application.ReleaseStockUseCase;
import com.company.ecommerce.inventory.application.ReserveStockUseCase;
import com.company.ecommerce.inventory.application.UpdateStockUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin inventory management. Requires {@code ROLE_ADMIN}. */
@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Inventory", description = "Stock management, reservation and release (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminInventoryController {

    private final GetStockUseCase getStockUseCase;
    private final UpdateStockUseCase updateStockUseCase;
    private final ReserveStockUseCase reserveStockUseCase;
    private final ReleaseStockUseCase releaseStockUseCase;

    @GetMapping("/{productId}")
    @Operation(summary = "Get stock levels for a product")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Stock returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Inventory not found for product")
    })
    public ApiResponse<InventoryResponse> get(@PathVariable UUID productId) {
        return ApiResponse.success(getStockUseCase.getByProductId(productId));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Set a product's on-hand stock")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Stock updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Inventory not found for product"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "On-hand below reserved quantity")
    })
    public ApiResponse<InventoryResponse> update(
            @PathVariable UUID productId, @Valid @RequestBody UpdateStockRequest request) {
        return ApiResponse.success("Stock updated", updateStockUseCase.update(productId, request));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock for a product")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Stock reserved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Inventory not found for product"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Insufficient stock available")
    })
    public ApiResponse<ReservationResponse> reserve(
            @Valid @RequestBody ReserveStockRequest request) {
        return ApiResponse.success("Stock reserved", reserveStockUseCase.reserve(request));
    }

    @PostMapping("/release")
    @Operation(summary = "Release a stock reservation")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Stock released"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Reservation not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Reservation already released")
    })
    public ApiResponse<ReservationResponse> release(
            @Valid @RequestBody ReleaseStockRequest request) {
        return ApiResponse.success("Stock released", releaseStockUseCase.release(request));
    }
}