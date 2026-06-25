package com.company.ecommerce.coupon.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.coupon.api.dto.CreateCouponRequest;
import com.company.ecommerce.coupon.domain.Coupon;
import com.company.ecommerce.coupon.domain.DiscountType;
import com.company.ecommerce.coupon.infrastructure.mapper.CouponMapper;
import com.company.ecommerce.coupon.infrastructure.persistence.CouponRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateCouponUseCaseTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CouponMapper couponMapper;
    @InjectMocks private CreateCouponUseCase useCase;

    private final Instant now = Instant.now();

    private CreateCouponRequest request() {
        return new CreateCouponRequest(
                "SAVE20",
                "20% off",
                DiscountType.PERCENTAGE,
                new BigDecimal("20"),
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                now,
                now.plus(Duration.ofDays(30)),
                1000);
    }

    @Test
    void createsCouponWhenCodeAvailable() {
        when(couponRepository.existsByCode("SAVE20")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.create(request());

        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void rejectsDuplicateCode() {
        when(couponRepository.existsByCode("SAVE20")).thenReturn(true);

        assertThatThrownBy(() -> useCase.create(request()))
                .isInstanceOf(BusinessException.class);
    }
}
