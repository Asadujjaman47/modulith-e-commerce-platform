package com.company.ecommerce.reporting.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.common.api.PageResponse;
import com.company.ecommerce.reporting.api.dto.ProductSalesResponse;
import com.company.ecommerce.reporting.api.dto.SalesReportResponse;
import com.company.ecommerce.reporting.application.ProductReportUseCase;
import com.company.ecommerce.reporting.application.SalesReportUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin reporting endpoints: sales and product reports built from order events. */
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Reports", description = "Sales and product reporting (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportController {

    private final SalesReportUseCase salesReportUseCase;
    private final ProductReportUseCase productReportUseCase;

    @GetMapping("/sales")
    @Operation(
            summary = "Sales report",
            description =
                    "Returns grand totals and a per-day breakdown of orders, units sold, revenue and"
                            + " discounts over an inclusive date window.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Sales report returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Start date is after the end date"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Caller is not an admin")
    })
    public ApiResponse<SalesReportResponse> sales(
            @Parameter(description = "Inclusive start date (yyyy-MM-dd)", required = true)
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @Parameter(description = "Inclusive end date (yyyy-MM-dd)", required = true)
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to) {
        return ApiResponse.success(salesReportUseCase.report(from, to));
    }

    @GetMapping("/products")
    @Operation(
            summary = "Product report",
            description =
                    "Returns the top products by units sold over an inclusive date window, paginated."
                            + " Order events carry no unit price, so revenue is not reported per product.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Product report returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Start date is after the end date"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Caller is not an admin")
    })
    public ApiResponse<PageResponse<ProductSalesResponse>> products(
            @Parameter(description = "Inclusive start date (yyyy-MM-dd)", required = true)
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @Parameter(description = "Inclusive end date (yyyy-MM-dd)", required = true)
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @ParameterObject Pageable pageable) {
        return ApiResponse.success(productReportUseCase.report(from, to, pageable));
    }
}
