package com.company.ecommerce.order.api.dto;

import com.company.ecommerce.order.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Admin request to transition an order to a new status. */
@Schema(description = "Update-order-status request")
public record UpdateOrderStatusRequest(
        @Schema(description = "Target status", example = "PROCESSING") @NotNull OrderStatus status) {}
