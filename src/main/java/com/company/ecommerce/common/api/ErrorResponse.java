package com.company.ecommerce.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
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
 *
 * <p>For unexpected server errors a {@code traceId} (the current Micrometer trace id) is included so
 * a failing request can be correlated with the logs; it is omitted from ordinary 4xx responses.
 */
@Schema(description = "Standard API error response envelope")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "Always false for errors", example = "false") boolean success,
        @Schema(description = "Human-readable error message", example = "Validation failed")
                String message,
        @Schema(description = "Field-level validation errors") List<FieldError> errors,
        @Schema(description = "Trace id for correlating server errors with logs", example = "a1b2c3d4")
                String traceId) {

    /** A single field-level validation error. */
    @Schema(description = "Field-level validation error")
    public record FieldError(
            @Schema(description = "Name of the offending field", example = "email") String field,
            @Schema(description = "Why the field is invalid", example = "must not be blank")
                    String message) {}

    public static ErrorResponse of(String message, List<FieldError> errors) {
        return new ErrorResponse(false, message, errors, null);
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse(false, message, List.of(), null);
    }

    /** Error envelope carrying a trace id, used for unexpected server errors. */
    public static ErrorResponse of(String message, String traceId) {
        return new ErrorResponse(false, message, List.of(), traceId);
    }
}