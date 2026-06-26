package com.company.ecommerce.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.coupon.domain.Coupon;
import com.company.ecommerce.coupon.domain.CouponUsage;
import com.company.ecommerce.coupon.domain.DiscountType;
import com.company.ecommerce.coupon.domain.event.CouponAppliedEvent;
import com.company.ecommerce.coupon.infrastructure.persistence.CouponRepository;
import com.company.ecommerce.coupon.infrastructure.persistence.CouponUsageRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CouponRedemptionServiceTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CouponUsageRepository usageRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private CouponRedemptionService service;

    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    private Coupon coupon() {
        return Coupon.create(
                "SAVE20",
                "20% off",
                DiscountType.PERCENTAGE,
                new BigDecimal("20"),
                null,
                null,
                Instant.now().minus(1, ChronoUnit.DAYS),
                Instant.now().plus(1, ChronoUnit.DAYS),
                100);
    }

    @Test
    void recordsUsageAndPublishes() {
        Coupon coupon = coupon();
        when(couponRepository.findByCode("SAVE20")).thenReturn(Optional.of(coupon));

        service.redeem("SAVE20", customerId, orderId, new BigDecimal("20.00"));

        assertThat(coupon.getTimesUsed()).isEqualTo(1);
        verify(usageRepository).save(any(CouponUsage.class));
        verify(eventPublisher).publishEvent(any(CouponAppliedEvent.class));
    }

    @Test
    void skipsUnknownCoupon() {
        when(couponRepository.findByCode("SAVE20")).thenReturn(Optional.empty());

        service.redeem("SAVE20", customerId, orderId, new BigDecimal("20.00"));

        verify(usageRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
