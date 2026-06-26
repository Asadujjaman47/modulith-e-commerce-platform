package com.company.ecommerce.shipment.infrastructure.persistence;

import com.company.ecommerce.shipment.domain.Shipment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Shipment} aggregates. Internal to the {@code shipment} module. */
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findByOrderId(UUID orderId);

    Optional<Shipment> findByIdAndCustomerId(UUID id, UUID customerId);

    Page<Shipment> findByCustomerId(UUID customerId, Pageable pageable);

    boolean existsByOrderId(UUID orderId);
}
