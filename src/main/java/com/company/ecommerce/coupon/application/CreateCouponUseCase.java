package com.company.ecommerce.coupon.application;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.coupon.api.dto.CouponResponse;
import com.company.ecommerce.coupon.api.dto.CreateCouponRequest;
import com.company.ecommerce.coupon.domain.Coupon;
import com.company.ecommerce.coupon.infrastructure.mapper.CouponMapper;
import com.company.ecommerce.coupon.infrastructure.persistence.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates a discount coupon (admin operation). */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateCouponUseCase {

    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;

    @Transactional
    public CouponResponse create(CreateCouponRequest request) {
        if (couponRepository.existsByCode(request.code())) {
            throw new BusinessException("Coupon code already exists: " + request.code());
        }
        Coupon coupon =
                couponRepository.save(
                        Coupon.create(
                                request.code(),
                                request.description(),
                                request.discountType(),
                                request.discountValue(),
                                request.minOrderAmount(),
                                request.maxDiscountAmount(),
                                request.validFrom(),
                                request.validUntil(),
                                request.usageLimit()));
        log.info("Coupon created. id={} code={}", coupon.getId(), coupon.getCode());
        return couponMapper.toResponse(coupon);
    }
}
