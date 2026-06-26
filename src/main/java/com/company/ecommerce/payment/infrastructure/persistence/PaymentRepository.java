package com.company.ecommerce.payment.infrastructure.persistence;

import com.company.ecommerce.payment.domain.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Payment} aggregates. Internal to the {@code payment} module. */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByIdAndCustomerId(UUID id, UUID customerId);

    Page<Payment> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Payment> findByCustomerIdAndOrderId(UUID customerId, UUID orderId, Pageable pageable);
}
