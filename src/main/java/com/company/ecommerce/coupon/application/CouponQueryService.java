package com.company.ecommerce.coupon.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.coupon.domain.Coupon;
import com.company.ecommerce.coupon.infrastructure.persistence.CouponRepository;
import com.company.ecommerce.coupon.spi.CouponQuery;
import com.company.ecommerce.coupon.spi.CouponQuote;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link CouponQuery} implementation. Purely read-only: it validates the coupon and computes
 * the discount but records no usage and performs no lazy expiry, so it is safe to call from another
 * module's transaction (e.g. {@code order} at checkout).
 */
@Service
@RequiredArgsConstructor
public class CouponQueryService implements CouponQuery {

    private final CouponRepository couponRepository;

    @Override
    @Transactional(readOnly = true)
    public CouponQuote quote(String code, BigDecimal orderAmount) {
        Coupon coupon =
                couponRepository
                        .findByCode(code)
                        .orElseThrow(() -> new EntityNotFoundException("Coupon", code));
        coupon.validateFor(orderAmount, Instant.now());
        return new CouponQuote(coupon.getCode(), coupon.calculateDiscount(orderAmount));
    }
}
