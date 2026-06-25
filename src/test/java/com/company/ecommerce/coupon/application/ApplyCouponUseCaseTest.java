package com.company.ecommerce.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.coupon.api.dto.ApplyCouponRequest;
import com.company.ecommerce.coupon.api.dto.CouponApplicationResponse;
import com.company.ecommerce.coupon.domain.Coupon;
import com.company.ecommerce.coupon.domain.CouponUsage;
import com.company.ecommerce.coupon.domain.DiscountType;
import com.company.ecommerce.coupon.domain.event.CouponAppliedEvent;
import com.company.ecommerce.coupon.infrastructure.persistence.CouponUsageRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ApplyCouponUseCaseTest {

    @Mock private CouponReader couponReader;
    @Mock private CouponUsageRepository usageRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private ApplyCouponUseCase useCase;

    private final Instant now = Instant.now();
    private final UUID customerId = UUID.randomUUID();

    private Coupon validCoupon() {
        return Coupon.create(
                "SAVE20",
                "20% off",
                DiscountType.PERCENTAGE,
                new BigDecimal("20"),
                null,
                null,
                now.minus(Duration.ofDays(1)),
                now.plus(Duration.ofDays(1)),
                5);
    }

    @Test
    void appliesCouponRecordsUsageAndPublishesEvent() {
        Coupon coupon = validCoupon();
        when(couponReader.requireByCode(eq("SAVE20"), any())).thenReturn(coupon);

        CouponApplicationResponse response =
                useCase.apply(customerId, new ApplyCouponRequest("SAVE20", new BigDecimal("150.00")));

        assertThat(response.discountAmount()).isEqualByComparingTo("30.00");
        assertThat(response.finalAmount()).isEqualByComparingTo("120.00");
        assertThat(coupon.getTimesUsed()).isEqualTo(1);
        verify(usageRepository).save(any(CouponUsage.class));
        verify(eventPublisher).publishEvent(any(CouponAppliedEvent.class));
    }
}
