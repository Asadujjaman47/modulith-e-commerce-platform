package com.company.ecommerce.payment.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.common.api.PageResponse;
import com.company.ecommerce.payment.api.dto.CreatePaymentRequest;
import com.company.ecommerce.payment.api.dto.PaymentResponse;
import com.company.ecommerce.payment.api.dto.PaymentSummaryResponse;
import com.company.ecommerce.payment.application.CreatePaymentUseCase;
import com.company.ecommerce.payment.application.GetPaymentUseCase;
import com.company.ecommerce.payment.application.ListPaymentsUseCase;
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

/** Authenticated customer payment endpoints: process a payment, view status and history. */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Authenticated customer payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final ListPaymentsUseCase listPaymentsUseCase;

    @PostMapping
    @Operation(
            summary = "Process a payment",
            description =
                    "Charges the authenticated customer's order through the payment gateway. Pass an"
                            + " Idempotency-Key header to make retries safe; paying an already-paid"
                            + " order returns the original payment.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Payment processed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Order cannot be paid (cancelled/refunded or no payable amount)")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> process(
            @Valid @RequestBody CreatePaymentRequest request,
            @Parameter(description = "Optional idempotency key to safely retry the request")
                    @RequestHeader(value = "Idempotency-Key", required = false)
                    String idempotencyKey) {
        PaymentResponse payment =
                createPaymentUseCase.process(CurrentUser.id(), request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed", payment));
    }

    @GetMapping
    @Operation(
            summary = "List my payments",
            description =
                    "Returns the authenticated customer's payment history, optionally filtered by"
                            + " order.")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Payments returned"))
    public ApiResponse<PageResponse<PaymentSummaryResponse>> list(
            @Parameter(description = "Filter by order id") @RequestParam(required = false)
                    UUID orderId,
            @ParameterObject Pageable pageable) {
        return ApiResponse.success(
                listPaymentsUseCase.listForCustomer(CurrentUser.id(), orderId, pageable));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get one of my payments")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Payment returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Payment not found")
    })
    public ApiResponse<PaymentResponse> get(@PathVariable UUID paymentId) {
        return ApiResponse.success(getPaymentUseCase.getForCustomer(CurrentUser.id(), paymentId));
    }
}
