package com.company.ecommerce.coupon.application;

import com.company.ecommerce.coupon.api.dto.ApplyCouponRequest;
import com.company.ecommerce.coupon.api.dto.CouponApplicationResponse;
import com.company.ecommerce.coupon.domain.Coupon;
import com.company.ecommerce.coupon.domain.CouponUsage;
import com.company.ecommerce.coupon.domain.event.CouponAppliedEvent;
import com.company.ecommerce.coupon.infrastructure.persistence.CouponUsageRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies a coupon to an order amount on behalf of a customer: validates it, computes the discount,
 * records the usage, and publishes {@link CouponAppliedEvent}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyCouponUseCase {

    private final CouponReader couponReader;
    private final CouponUsageRepository usageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CouponApplicationResponse apply(UUID customerId, ApplyCouponRequest request) {
        Instant now = Instant.now();
        Coupon coupon = couponReader.requireByCode(request.code(), now);
        coupon.validateFor(request.orderAmount(), now);

        BigDecimal discount = coupon.calculateDiscount(request.orderAmount());
        coupon.recordUsage();
        usageRepository.save(CouponUsage.record(coupon.getId(), customerId, discount));

        eventPublisher.publishEvent(
                new CouponAppliedEvent(coupon.getId(), coupon.getCode(), customerId, discount));
        log.info(
                "Coupon applied. code={} customerId={} discount={}",
                coupon.getCode(),
                customerId,
                discount);
        return new CouponApplicationResponse(
                coupon.getId(),
                coupon.getCode(),
                request.orderAmount(),
                discount,
                request.orderAmount().subtract(discount));
    }
}
