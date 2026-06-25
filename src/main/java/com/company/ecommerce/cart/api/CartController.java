package com.company.ecommerce.cart.api;

import com.company.ecommerce.cart.api.dto.AddToCartRequest;
import com.company.ecommerce.cart.api.dto.CartResponse;
import com.company.ecommerce.cart.api.dto.UpdateCartItemRequest;
import com.company.ecommerce.cart.application.AddToCartUseCase;
import com.company.ecommerce.cart.application.GetCartUseCase;
import com.company.ecommerce.cart.application.RemoveCartItemUseCase;
import com.company.ecommerce.cart.application.UpdateCartItemUseCase;
import com.company.ecommerce.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated customer shopping-cart endpoints. */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Authenticated customer shopping cart")
@SecurityRequirement(name = "bearerAuth")
public class CartController {

    private final GetCartUseCase getCartUseCase;
    private final AddToCartUseCase addToCartUseCase;
    private final UpdateCartItemUseCase updateCartItemUseCase;
    private final RemoveCartItemUseCase removeCartItemUseCase;

    @GetMapping
    @Operation(summary = "Get the authenticated customer's cart")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Cart returned"))
    public ApiResponse<CartResponse> get() {
        return ApiResponse.success(getCartUseCase.getCart(CurrentUser.id()));
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item to the cart")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Item added"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Product not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Product inactive or insufficient stock")
    })
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody AddToCartRequest request) {
        CartResponse cart = addToCartUseCase.add(CurrentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added", cart));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update the quantity of a cart item")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Item updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Cart item not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Insufficient stock")
    })
    public ApiResponse<CartResponse> updateItem(
            @PathVariable UUID itemId, @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success(
                "Item updated", updateCartItemUseCase.update(CurrentUser.id(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove an item from the cart")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Item removed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Cart item not found")
    })
    public ApiResponse<CartResponse> removeItem(@PathVariable UUID itemId) {
        return ApiResponse.success(
                "Item removed", removeCartItemUseCase.remove(CurrentUser.id(), itemId));
    }
}
