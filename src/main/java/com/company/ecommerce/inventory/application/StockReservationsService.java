package com.company.ecommerce.inventory.application;

import com.company.ecommerce.inventory.api.dto.ReleaseStockRequest;
import com.company.ecommerce.inventory.api.dto.ReserveStockRequest;
import com.company.ecommerce.inventory.domain.ReservationStatus;
import com.company.ecommerce.inventory.infrastructure.persistence.StockReservationRepository;
import com.company.ecommerce.inventory.spi.StockReservations;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Default {@link StockReservations} implementation. Delegates to the existing reserve/release use
 * cases so the same domain invariants and ledger entries apply, and looks up reservations by their
 * caller reference to release them as a group.
 */
@Service
@RequiredArgsConstructor
public class StockReservationsService implements StockReservations {

    private final ReserveStockUseCase reserveStockUseCase;
    private final ReleaseStockUseCase releaseStockUseCase;
    private final StockReservationRepository reservationRepository;

    @Override
    public void reserve(UUID productId, int quantity, String reference) {
        reserveStockUseCase.reserve(new ReserveStockRequest(productId, quantity, reference));
    }

    @Override
    public void releaseByReference(String reference) {
        reservationRepository
                .findByReferenceAndStatus(reference, ReservationStatus.ACTIVE)
                .forEach(
                        reservation ->
                                releaseStockUseCase.release(
                                        new ReleaseStockRequest(reservation.getId())));
    }
}
