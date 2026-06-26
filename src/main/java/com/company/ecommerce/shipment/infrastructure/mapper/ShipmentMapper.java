package com.company.ecommerce.shipment.infrastructure.mapper;

import com.company.ecommerce.shipment.api.dto.ShipmentResponse;
import com.company.ecommerce.shipment.api.dto.ShipmentResponse.DeliveryAddressResponse;
import com.company.ecommerce.shipment.api.dto.ShipmentSummaryResponse;
import com.company.ecommerce.shipment.api.dto.TrackingRecordResponse;
import com.company.ecommerce.shipment.domain.DeliveryAddress;
import com.company.ecommerce.shipment.domain.Shipment;
import com.company.ecommerce.shipment.domain.TrackingRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps shipment aggregates to response DTOs. */
@Mapper(componentModel = "spring")
public interface ShipmentMapper {

    ShipmentResponse toResponse(Shipment shipment);

    ShipmentSummaryResponse toSummaryResponse(Shipment shipment);

    DeliveryAddressResponse toAddressResponse(DeliveryAddress address);

    @Mapping(target = "occurredAt", source = "createdAt")
    TrackingRecordResponse toTrackingResponse(TrackingRecord record);
}
