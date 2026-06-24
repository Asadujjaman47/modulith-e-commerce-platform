package com.company.ecommerce.user.api;

import com.company.ecommerce.common.api.ApiResponse;
import com.company.ecommerce.user.api.dto.AddressResponse;
import com.company.ecommerce.user.api.dto.CreateAddressRequest;
import com.company.ecommerce.user.api.dto.UpdateAddressRequest;
import com.company.ecommerce.user.application.ManageAddressesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated customer address endpoints. */
@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
@Tag(name = "User Addresses", description = "Authenticated customer address management")
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final ManageAddressesUseCase manageAddressesUseCase;

    @GetMapping
    @Operation(summary = "List the authenticated customer's addresses")
    @ApiResponses(
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Addresses returned"))
    public ApiResponse<List<AddressResponse>> list() {
        return ApiResponse.success(manageAddressesUseCase.list(CurrentUser.id()));
    }

    @PostMapping
    @Operation(summary = "Add an address")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Address created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation failed")
    })
    public ResponseEntity<ApiResponse<AddressResponse>> add(
            @Valid @RequestBody CreateAddressRequest request) {
        AddressResponse response = manageAddressesUseCase.add(CurrentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address created", response));
    }

    @PutMapping("/{addressId}")
    @Operation(summary = "Update an address")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Address updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Address not found")
    })
    public ApiResponse<AddressResponse> update(
            @PathVariable UUID addressId, @Valid @RequestBody UpdateAddressRequest request) {
        return ApiResponse.success(
                "Address updated", manageAddressesUseCase.update(CurrentUser.id(), addressId, request));
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete an address")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "204",
                description = "Address deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Address not found")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID addressId) {
        manageAddressesUseCase.delete(CurrentUser.id(), addressId);
        return ResponseEntity.noContent().build();
    }
}