package com.company.ecommerce.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.ecommerce.inventory.api.dto.ReleaseStockRequest;
import com.company.ecommerce.inventory.api.dto.ReserveStockRequest;
import com.company.ecommerce.inventory.domain.ReservationStatus;
import com.company.ecommerce.inventory.domain.StockReservation;
import com.company.ecommerce.inventory.infrastructure.persistence.StockReservationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockReservationsServiceTest {

    @Mock private ReserveStockUseCase reserveStockUseCase;
    @Mock private ReleaseStockUseCase releaseStockUseCase;
    @Mock private StockReservationRepository reservationRepository;
    @InjectMocks private StockReservationsService service;

    private final UUID productId = UUID.randomUUID();
    private final String reference = UUID.randomUUID().toString();

    @Test
    void reservesViaUseCaseWithReference() {
        service.reserve(productId, 3, reference);

        ArgumentCaptor<ReserveStockRequest> captor =
                ArgumentCaptor.forClass(ReserveStockRequest.class);
        verify(reserveStockUseCase).reserve(captor.capture());
        assertThat(captor.getValue().productId()).isEqualTo(productId);
        assertThat(captor.getValue().quantity()).isEqualTo(3);
        assertThat(captor.getValue().reference()).isEqualTo(reference);
    }

    @Test
    void releasesAllActiveReservationsForReference() {
        StockReservation reservation = StockReservation.create(productId, 3, reference);
        when(reservationRepository.findByReferenceAndStatus(reference, ReservationStatus.ACTIVE))
                .thenReturn(List.of(reservation));

        service.releaseByReference(reference);

        ArgumentCaptor<ReleaseStockRequest> captor =
                ArgumentCaptor.forClass(ReleaseStockRequest.class);
        verify(releaseStockUseCase).release(captor.capture());
        assertThat(captor.getValue().reservationId()).isEqualTo(reservation.getId());
    }
}
