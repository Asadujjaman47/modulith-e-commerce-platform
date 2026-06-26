package com.company.ecommerce.shipment.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.shipment.api.dto.CreateShipmentRequest;
import com.company.ecommerce.shipment.api.dto.ShipmentResponse;
import com.company.ecommerce.shipment.api.dto.UpdateShipmentStatusRequest;
import com.company.ecommerce.shipment.application.CreateShipmentUseCase;
import com.company.ecommerce.shipment.application.MarkDeliveredUseCase;
import com.company.ecommerce.shipment.application.TrackShipmentUseCase;
import com.company.ecommerce.shipment.application.UpdateShipmentStatusUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin shipment management: create shipments, advance status, view any shipment, confirm delivery. */
@RestController
@RequestMapping("/api/v1/admin/shipments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Shipments", description = "Shipment management (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminShipmentController {

    private final CreateShipmentUseCase createShipmentUseCase;
    private final UpdateShipmentStatusUseCase updateShipmentStatusUseCase;
    private final MarkDeliveredUseCase markDeliveredUseCase;
    private final TrackShipmentUseCase trackShipmentUseCase;

    @PostMapping
    @Operation(
            summary = "Create a shipment",
            description =
                    "Creates a shipment for a paid order. Idempotent: if the order already has a"
                            + " shipment, that shipment is returned.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Shipment created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Caller is not an admin"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Order not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Order is not paid")
    })
    public ResponseEntity<ApiResponse<ShipmentResponse>> create(
            @Valid @RequestBody CreateShipmentRequest request) {
        ShipmentResponse shipment = createShipmentUseCase.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipment created", shipment));
    }

    @GetMapping("/{shipmentId}")
    @Operation(summary = "Get any shipment")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Shipment returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Shipment not found")
    })
    public ApiResponse<ShipmentResponse> get(@PathVariable UUID shipmentId) {
        return ApiResponse.success(trackShipmentUseCase.getById(shipmentId));
    }

    @PutMapping("/{shipmentId}/status")
    @Operation(
            summary = "Update shipment status",
            description =
                    "Advances the shipment to the next status, enforcing the allowed-transition"
                            + " rules and recording a tracking entry. Setting DELIVERED confirms"
                            + " delivery.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Shipment not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Illegal status transition")
    })
    public ApiResponse<ShipmentResponse> updateStatus(
            @PathVariable UUID shipmentId, @Valid @RequestBody UpdateShipmentStatusRequest request) {
        return ApiResponse.success(
                "Status updated", updateShipmentStatusUseCase.updateStatus(shipmentId, request));
    }

    @PostMapping("/{shipmentId}/deliver")
    @Operation(
            summary = "Confirm delivery",
            description = "Marks the shipment as delivered and completes the order.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Shipment delivered"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Shipment not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Shipment is already delivered")
    })
    public ApiResponse<ShipmentResponse> deliver(@PathVariable UUID shipmentId) {
        return ApiResponse.success("Shipment delivered", markDeliveredUseCase.deliver(shipmentId));
    }
}
