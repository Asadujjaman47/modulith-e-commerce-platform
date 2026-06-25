package com.company.ecommerce.inventory.infrastructure.persistence;

import com.company.ecommerce.inventory.domain.StockReservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link StockReservation} aggregates. */
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {}