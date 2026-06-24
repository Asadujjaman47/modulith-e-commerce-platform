package com.company.ecommerce.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Standard error envelope returned by {@code GlobalExceptionHandler}.
 *
 * <pre>
 * {
 *   "success": false,
 *   "message": "Validation failed",
 *   "errors": [ ... ]
 * }
 * </pre>
 */
@Schema(description = "Standard API error response envelope")
public record ErrorResponse(
        @Schema(description = "Always false for errors", example = "false") boolean success,
        @Schema(description = "Human-readable error message", example = "Validation failed")
                String message,
        @Schema(description = "Detailed error messages") List<String> errors) {

    public static ErrorResponse of(String message, List<String> errors) {
        return new ErrorResponse(false, message, errors);
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse(false, message, List.of());
    }
}