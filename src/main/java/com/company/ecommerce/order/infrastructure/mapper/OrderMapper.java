package com.company.ecommerce.order.infrastructure.mapper;

import com.company.ecommerce.order.api.dto.OrderAddressResponse;
import com.company.ecommerce.order.api.dto.OrderItemResponse;
import com.company.ecommerce.order.api.dto.OrderResponse;
import com.company.ecommerce.order.api.dto.OrderSummaryResponse;
import com.company.ecommerce.order.domain.Order;
import com.company.ecommerce.order.domain.OrderAddress;
import com.company.ecommerce.order.domain.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps order aggregates to response DTOs. */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    @Mapping(target = "itemCount", expression = "java(order.getItems().size())")
    OrderSummaryResponse toSummaryResponse(Order order);

    @Mapping(target = "lineTotal", expression = "java(item.lineTotal())")
    OrderItemResponse toItemResponse(OrderItem item);

    OrderAddressResponse toAddressResponse(OrderAddress address);
}
