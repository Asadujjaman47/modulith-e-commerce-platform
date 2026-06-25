package com.company.ecommerce.cart.infrastructure.mapper;

import com.company.ecommerce.cart.api.dto.CartItemResponse;
import com.company.ecommerce.cart.api.dto.CartResponse;
import com.company.ecommerce.cart.domain.Cart;
import com.company.ecommerce.cart.domain.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps cart aggregates to response DTOs. */
@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "subtotal", expression = "java(cart.subtotal())")
    CartResponse toResponse(Cart cart);

    @Mapping(target = "lineTotal", expression = "java(item.lineTotal())")
    CartItemResponse toItemResponse(CartItem item);
}
