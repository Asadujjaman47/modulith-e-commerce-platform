package com.company.ecommerce.coupon.application;

import com.company.ecommerce.common.exception.EntityNotFoundException;
import com.company.ecommerce.coupon.domain.Coupon;
import com.company.ecommerce.coupon.domain.event.CouponExpiredEvent;
import com.company.ecommerce.coupon.infrastructure.persistence.CouponRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Loads a coupon by code and lazily expires it: an active coupon found past its validity window is
 * deactivated and a {@link CouponExpiredEvent} is published. Shared by validate/apply.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class CouponReader {

    private final CouponRepository couponRepository;
    private final ApplicationEventPublisher eventPublisher;

    Coupon requireByCode(String code, Instant now) {
        Coupon coupon =
                couponRepository
                        .findByCode(code)
                        .orElseThrow(() -> new EntityNotFoundException("Coupon", code));
        if (coupon.isActive() && coupon.isExpired(now)) {
            coupon.deactivate();
            eventPublisher.publishEvent(new CouponExpiredEvent(coupon.getId(), coupon.getCode()));
            log.info("Coupon expired and deactivated. id={} code={}", coupon.getId(), code);
        }
        return coupon;
    }
}
