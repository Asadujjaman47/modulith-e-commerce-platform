package com.company.ecommerce.order.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.common.api.PageResponse;
import com.company.ecommerce.order.api.dto.OrderResponse;
import com.company.ecommerce.order.api.dto.OrderSummaryResponse;
import com.company.ecommerce.order.api.dto.PlaceOrderRequest;
import com.company.ecommerce.order.application.CancelOrderUseCase;
import com.company.ecommerce.order.application.GetOrderUseCase;
import com.company.ecommerce.order.application.ListOrdersUseCase;
import com.company.ecommerce.order.application.PlaceOrderUseCase;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated customer order endpoints: place, list (history), view and cancel own orders. */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Authenticated customer orders")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;
    private final ListOrdersUseCase listOrdersUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

    @PostMapping
    @Operation(
            summary = "Place an order",
            description =
                    "Creates an order from the authenticated customer's current cart. Optionally"
                            + " applies a coupon. Pass an Idempotency-Key header to make retries"
                            + " safe.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Order placed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Address or coupon not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Empty cart, insufficient stock or invalid coupon")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> place(
            @Valid @RequestBody PlaceOrderRequest request,
            @Parameter(description = "Optional idempotency key to safely retry the request")
                    @RequestHeader(value = "Idempotency-Key", required = false)
                    String idempotencyKey) {
        OrderResponse order = placeOrderUseCase.place(CurrentUser.id(), request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed", order));
    }

    @GetMapping
    @Operation(
            summary = "List my orders",
            description = "Returns the authenticated customer's order history, optionally by status.")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Orders returned"))
    public ApiResponse<PageResponse<OrderSummaryResponse>> list(
            @Parameter(description = "Filter by status") @RequestParam(required = false)
                    OrderStatus status,
            @ParameterObject Pageable pageable) {
        return ApiResponse.success(
                listOrdersUseCase.listForCustomer(CurrentUser.id(), status, pageable));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get one of my orders")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Order returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Order not found")
    })
    public ApiResponse<OrderResponse> get(@PathVariable UUID orderId) {
        return ApiResponse.success(getOrderUseCase.getForCustomer(CurrentUser.id(), orderId));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(
            summary = "Cancel one of my orders",
            description = "Cancels the order if its current status still allows cancellation.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Order cancelled"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Order cannot be cancelled in its current status")
    })
    public ApiResponse<OrderResponse> cancel(@PathVariable UUID orderId) {
        return ApiResponse.success("Order cancelled", cancelOrderUseCase.cancel(CurrentUser.id(), orderId));
    }
}
