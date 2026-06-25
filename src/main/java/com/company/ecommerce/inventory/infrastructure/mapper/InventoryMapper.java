package com.company.ecommerce.inventory.infrastructure.mapper;

import com.company.ecommerce.inventory.api.dto.InventoryResponse;
import com.company.ecommerce.inventory.api.dto.ReservationResponse;
import com.company.ecommerce.inventory.domain.Inventory;
import com.company.ecommerce.inventory.domain.StockReservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps inventory aggregates to response DTOs. */
@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "quantityAvailable", expression = "java(inventory.available())")
    InventoryResponse toResponse(Inventory inventory);

    @Mapping(target = "reservationId", source = "id")
    ReservationResponse toResponse(StockReservation reservation);
}