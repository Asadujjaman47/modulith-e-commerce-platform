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
 *   "errors": [ { "field": "email", "message": "must not be blank" } ]
 * }
 * </pre>
 */
@Schema(description = "Standard API error response envelope")
public record ErrorResponse(
        @Schema(description = "Always false for errors", example = "false") boolean success,
        @Schema(description = "Human-readable error message", example = "Validation failed")
                String message,
        @Schema(description = "Field-level validation errors") List<FieldError> errors) {

    /** A single field-level validation error. */
    @Schema(description = "Field-level validation error")
    public record FieldError(
            @Schema(description = "Name of the offending field", example = "email") String field,
            @Schema(description = "Why the field is invalid", example = "must not be blank")
                    String message) {}

    public static ErrorResponse of(String message, List<FieldError> errors) {
        return new ErrorResponse(false, message, errors);
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse(false, message, List.of());
    }
}