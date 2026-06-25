package com.company.ecommerce.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.coupon.api.dto.CouponValidationResponse;
import com.company.ecommerce.coupon.api.dto.ValidateCouponRequest;
import com.company.ecommerce.coupon.domain.Coupon;
import com.company.ecommerce.coupon.domain.DiscountType;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidateCouponUseCaseTest {

    @Mock private CouponReader couponReader;
    @InjectMocks private ValidateCouponUseCase useCase;

    private final Instant now = Instant.now();

    private Coupon validCoupon() {
        return Coupon.create(
                "SAVE20",
                "20% off",
                DiscountType.PERCENTAGE,
                new BigDecimal("20"),
                new BigDecimal("100.00"),
                null,
                now.minus(Duration.ofDays(1)),
                now.plus(Duration.ofDays(1)),
                null);
    }

    @Test
    void returnsDiscountForValidCoupon() {
        when(couponReader.requireByCode(eq("SAVE20"), any())).thenReturn(validCoupon());

        CouponValidationResponse response =
                useCase.validate(new ValidateCouponRequest("SAVE20", new BigDecimal("150.00")));

        assertThat(response.valid()).isTrue();
        assertThat(response.discountAmount()).isEqualByComparingTo("30.00");
        assertThat(response.finalAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void propagatesValidationFailure() {
        when(couponReader.requireByCode(eq("SAVE20"), any())).thenReturn(validCoupon());

        assertThatThrownBy(
                        () ->
                                useCase.validate(
                                        new ValidateCouponRequest("SAVE20", new BigDecimal("50.00"))))
                .isInstanceOf(BusinessException.class);
    }
}
