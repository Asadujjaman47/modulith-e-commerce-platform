package com.company.ecommerce.shipment.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.shipment.api.dto.ShipmentResponse;
import com.company.ecommerce.shipment.application.TrackShipmentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated customer shipment endpoint: track one of your own shipments. */
@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipments", description = "Authenticated customer shipment tracking")
@SecurityRequirement(name = "bearerAuth")
public class ShipmentController {

    private final TrackShipmentUseCase trackShipmentUseCase;

    @GetMapping("/{shipmentId}")
    @Operation(
            summary = "Track one of my shipments",
            description = "Returns the shipment and its full tracking history if it belongs to you.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Shipment returned"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Shipment not found")
    })
    public ApiResponse<ShipmentResponse> track(@PathVariable UUID shipmentId) {
        return ApiResponse.success(trackShipmentUseCase.trackForCustomer(CurrentUser.id(), shipmentId));
    }
}
