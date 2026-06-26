package com.company.ecommerce.audit.api;

import com.company.ecommerce.audit.api.dto.AuditLogResponse;
import com.company.ecommerce.audit.application.AuditSearchCriteria;
import com.company.ecommerce.audit.application.GetUserActivityUseCase;
import com.company.ecommerce.audit.application.SearchAuditLogUseCase;
import com.company.ecommerce.audit.domain.AuditCategory;
import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin audit endpoints: search the audit trail and view a user's activity timeline. */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Audit", description = "Audit trail search and user activity (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminAuditController {

    private final SearchAuditLogUseCase searchAuditLogUseCase;
    private final GetUserActivityUseCase getUserActivityUseCase;

    @GetMapping
    @Operation(
            summary = "Search audit logs",
            description =
                    "Returns audit-trail entries (most recent first) filtered by any combination of"
                            + " category, event type, entity id, actor id and an occurred-at window.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Audit logs returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Caller is not an admin")
    })
    public ApiResponse<PageResponse<AuditLogResponse>> search(
            @Parameter(description = "Filter by functional area") @RequestParam(required = false)
                    AuditCategory category,
            @Parameter(description = "Filter by event type, e.g. OrderCreated")
                    @RequestParam(required = false)
                    String eventType,
            @Parameter(description = "Filter by affected entity id") @RequestParam(required = false)
                    UUID entityId,
            @Parameter(description = "Filter by acting customer id") @RequestParam(required = false)
                    UUID actorId,
            @Parameter(description = "Inclusive lower bound on occurredAt (ISO-8601)")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @Parameter(description = "Inclusive upper bound on occurredAt (ISO-8601)")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @ParameterObject Pageable pageable) {
        AuditSearchCriteria criteria =
                new AuditSearchCriteria(category, eventType, entityId, actorId, from, to);
        return ApiResponse.success(searchAuditLogUseCase.search(criteria, pageable));
    }

    @GetMapping("/activity/{userId}")
    @Operation(
            summary = "User activity",
            description =
                    "Returns the audit-trail activity for a single user (as the acting customer),"
                            + " most recent first.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "User activity returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Caller is not an admin")
    })
    public ApiResponse<PageResponse<AuditLogResponse>> userActivity(
            @PathVariable UUID userId, @ParameterObject Pageable pageable) {
        return ApiResponse.success(getUserActivityUseCase.forUser(userId, pageable));
    }
}
