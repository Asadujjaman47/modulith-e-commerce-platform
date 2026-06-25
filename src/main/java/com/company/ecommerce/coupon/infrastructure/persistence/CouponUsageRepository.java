package com.company.ecommerce.coupon.infrastructure.persistence;

import com.company.ecommerce.coupon.domain.CouponUsage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link CouponUsage} records. */
public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {

    long countByCouponIdAndCustomerId(UUID couponId, UUID customerId);
}
