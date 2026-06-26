package com.company.ecommerce.payment.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.payment.api.dto.PaymentResponse;
import com.company.ecommerce.payment.application.GetPaymentUseCase;
import com.company.ecommerce.payment.application.RefundPaymentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin payment management: view any payment and issue refunds. */
@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Payments", description = "Payment management (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminPaymentController {

    private final GetPaymentUseCase getPaymentUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get any payment")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Payment returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Caller is not an admin"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Payment not found")
    })
    public ApiResponse<PaymentResponse> get(@PathVariable UUID paymentId) {
        return ApiResponse.success(getPaymentUseCase.getById(paymentId));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(
            summary = "Refund a payment",
            description =
                    "Refunds a successful payment. Idempotent: refunding an already-refunded payment"
                            + " returns it unchanged.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Payment refunded"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Caller is not an admin"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Payment not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Payment is not in a refundable state")
    })
    public ApiResponse<PaymentResponse> refund(
            @PathVariable UUID paymentId,
            @Parameter(description = "Optional idempotency key to safely retry the request")
                    @RequestHeader(value = "Idempotency-Key", required = false)
                    String idempotencyKey) {
        return ApiResponse.success("Payment refunded", refundPaymentUseCase.refund(paymentId));
    }
}
