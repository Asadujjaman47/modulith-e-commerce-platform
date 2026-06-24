package com.company.ecommerce.user.infrastructure.mapper;

import com.company.ecommerce.user.api.dto.CustomerResponse;
import com.company.ecommerce.user.domain.Customer;
import org.mapstruct.Mapper;

/** Maps {@link Customer} aggregates to {@link CustomerResponse} DTOs. */
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);
}