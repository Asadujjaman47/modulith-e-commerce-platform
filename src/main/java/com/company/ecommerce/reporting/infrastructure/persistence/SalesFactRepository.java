package com.company.ecommerce.reporting.infrastructure.persistence;

import com.company.ecommerce.reporting.application.DailySalesRow;
import com.company.ecommerce.reporting.application.SalesTotalsRow;
import com.company.ecommerce.reporting.domain.SalesFact;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence and aggregation for {@link SalesFact} rows. */
public interface SalesFactRepository extends JpaRepository<SalesFact, UUID> {

    boolean existsByOrderId(UUID orderId);

    @Query(
            """
            SELECT new com.company.ecommerce.reporting.application.DailySalesRow(
                f.orderDate,
                COUNT(f),
                COALESCE(SUM(f.itemCount), 0),
                COALESCE(SUM(f.orderTotal), 0),
                COALESCE(SUM(f.discountTotal), 0))
            FROM SalesFact f
            WHERE f.orderDate BETWEEN :from AND :to
            GROUP BY f.orderDate
            ORDER BY f.orderDate
            """)
    List<DailySalesRow> dailyBreakdown(
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(
            """
            SELECT new com.company.ecommerce.reporting.application.SalesTotalsRow(
                COUNT(f),
                COALESCE(SUM(f.itemCount), 0),
                COALESCE(SUM(f.orderTotal), 0),
                COALESCE(SUM(f.discountTotal), 0))
            FROM SalesFact f
            WHERE f.orderDate BETWEEN :from AND :to
            """)
    SalesTotalsRow totals(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
