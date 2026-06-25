package com.company.ecommerce.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Standard pagination envelope for collection endpoints, matching the platform API guide.
 *
 * <pre>
 * {
 *   "content": [ ... ],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 100,
 *   "totalPages": 5,
 *   "first": true,
 *   "last": false
 * }
 * </pre>
 *
 * @param <T> element type
 */
@Schema(description = "Standard paginated collection envelope")
public record PageResponse<T>(
        @Schema(description = "Page contents") List<T> content,
        @Schema(description = "Zero-based page index", example = "0") int page,
        @Schema(description = "Page size", example = "20") int size,
        @Schema(description = "Total number of matching elements", example = "100")
                long totalElements,
        @Schema(description = "Total number of pages", example = "5") int totalPages,
        @Schema(description = "Whether this is the first page", example = "true") boolean first,
        @Schema(description = "Whether this is the last page", example = "false") boolean last) {

    /** Wraps a Spring Data {@link Page} into the standard envelope. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
