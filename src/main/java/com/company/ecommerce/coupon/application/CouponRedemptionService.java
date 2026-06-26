package com.company.ecommerce.coupon.application;

import com.company.ecommerce.coupon.domain.Coupon;
import com.company.ecommerce.coupon.domain.CouponUsage;
import com.company.ecommerce.coupon.domain.event.CouponAppliedEvent;
import com.company.ecommerce.coupon.infrastructure.persistence.CouponRepository;
import com.company.ecommerce.coupon.infrastructure.persistence.CouponUsageRepository;
import com.company.ecommerce.coupon.spi.CouponRedemption;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link CouponRedemption} implementation. Records coupon usage linked to a concrete order,
 * increments the usage count and publishes {@link CouponAppliedEvent}. This is the usage-recording
 * path for orders (the order computed the discount via {@link CouponQueryService}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedemptionService implements CouponRedemption {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository usageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void redeem(String code, UUID customerId, UUID orderId, BigDecimal discountAmount) {
        Coupon coupon = couponRepository.findByCode(code).orElse(null);
        if (coupon == null) {
            log.warn("Order {} referenced unknown coupon {}; skipping usage record.", orderId, code);
            return;
        }

        coupon.recordUsage();
        usageRepository.save(
                CouponUsage.record(coupon.getId(), customerId, orderId, discountAmount));
        eventPublisher.publishEvent(
                new CouponAppliedEvent(coupon.getId(), coupon.getCode(), customerId, discountAmount));
        log.info(
                "Coupon redeemed for order. code={} orderId={} discount={}",
                code,
                orderId,
                discountAmount);
    }
}
