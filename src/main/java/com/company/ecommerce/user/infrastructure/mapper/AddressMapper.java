package com.company.ecommerce.user.infrastructure.mapper;

import com.company.ecommerce.user.api.dto.AddressResponse;
import com.company.ecommerce.user.domain.Address;
import java.util.List;
import org.mapstruct.Mapper;

/** Maps {@link Address} aggregates to {@link AddressResponse} DTOs. */
@Mapper(componentModel = "spring")
public interface AddressMapper {

    AddressResponse toResponse(Address address);

    List<AddressResponse> toResponseList(List<Address> addresses);
}