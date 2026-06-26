package com.company.ecommerce.order.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.common.api.PageResponse;
import com.company.ecommerce.order.api.dto.OrderResponse;
import com.company.ecommerce.order.api.dto.OrderSummaryResponse;
import com.company.ecommerce.order.api.dto.UpdateOrderStatusRequest;
import com.company.ecommerce.order.application.GetOrderUseCase;
import com.company.ecommerce.order.application.ListOrdersUseCase;
import com.company.ecommerce.order.application.UpdateOrderStatusUseCase;
import com.company.ecommerce.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin order management: browse all orders and drive the order status lifecycle. */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Orders", description = "Order management (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    private final ListOrdersUseCase listOrdersUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @GetMapping
    @Operation(summary = "List all orders", description = "Returns all orders, optionally by status.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Orders returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Caller is not an admin")
    })
    public ApiResponse<PageResponse<OrderSummaryResponse>> list(
            @Parameter(description = "Filter by status") @RequestParam(required = false)
                    OrderStatus status,
            @ParameterObject Pageable pageable) {
        return ApiResponse.success(listOrdersUseCase.listAll(status, pageable));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get any order")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Order returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Order not found")
    })
    public ApiResponse<OrderResponse> get(@PathVariable UUID orderId) {
        return ApiResponse.success(getOrderUseCase.getById(orderId));
    }

    @PutMapping("/{orderId}/status")
    @Operation(
            summary = "Update order status",
            description =
                    "Transitions the order to a new status, enforcing the allowed-transition rules.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Illegal status transition")
    })
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable UUID orderId, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.success(
                "Status updated", updateOrderStatusUseCase.updateStatus(orderId, request.status()));
    }
}
