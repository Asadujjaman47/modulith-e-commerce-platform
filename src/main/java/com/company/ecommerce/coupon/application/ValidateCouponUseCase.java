package com.company.ecommerce.coupon.application;

import com.company.ecommerce.coupon.api.dto.CouponValidationResponse;
import com.company.ecommerce.coupon.api.dto.ValidateCouponRequest;
import com.company.ecommerce.coupon.domain.Coupon;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validates a coupon against an order amount, returning the discount it would yield. Throws a
 * business error (HTTP 409) describing why an invalid coupon cannot be applied.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidateCouponUseCase {

    private final CouponReader couponReader;

    @Transactional
    public CouponValidationResponse validate(ValidateCouponRequest request) {
        Instant now = Instant.now();
        Coupon coupon = couponReader.requireByCode(request.code(), now);
        coupon.validateFor(request.orderAmount(), now);

        BigDecimal discount = coupon.calculateDiscount(request.orderAmount());
        log.info("Coupon validated. code={} discount={}", coupon.getCode(), discount);
        return new CouponValidationResponse(
                true,
                coupon.getCode(),
                coupon.getDiscountType(),
                request.orderAmount(),
                discount,
                request.orderAmount().subtract(discount));
    }
}
