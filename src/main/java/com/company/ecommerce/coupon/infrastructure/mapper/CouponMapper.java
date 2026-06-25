package com.company.ecommerce.coupon.infrastructure.mapper;

import com.company.ecommerce.coupon.api.dto.CouponResponse;
import com.company.ecommerce.coupon.domain.Coupon;
import org.mapstruct.Mapper;

/** Maps {@link Coupon} aggregates to {@link CouponResponse} DTOs. */
@Mapper(componentModel = "spring")
public interface CouponMapper {

    CouponResponse toResponse(Coupon coupon);
}
