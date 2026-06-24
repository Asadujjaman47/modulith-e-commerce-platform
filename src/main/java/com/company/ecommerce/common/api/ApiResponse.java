package com.company.ecommerce.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard success envelope returned by every controller.
 *
 * <pre>
 * {
 *   "success": true,
 *   "message": "Success",
 *   "data": { ... }
 * }
 * </pre>
 *
 * @param <T> payload type
 */
@Schema(description = "Standard API success response envelope")
public record ApiResponse<T>(
        @Schema(description = "Indicates the request succeeded", example = "true") boolean success,
        @Schema(description = "Human-readable message", example = "Success") String message,
        @Schema(description = "Response payload") T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
}