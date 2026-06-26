package com.company.ecommerce.inventory.infrastructure.persistence;

import com.company.ecommerce.inventory.domain.ReservationStatus;
import com.company.ecommerce.inventory.domain.StockReservation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link StockReservation} aggregates. */
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    /** All reservations created for the given external reference (e.g. an order id) in a status. */
    List<StockReservation> findByReferenceAndStatus(String reference, ReservationStatus status);
}