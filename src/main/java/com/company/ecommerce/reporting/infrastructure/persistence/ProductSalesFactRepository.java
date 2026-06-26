package com.company.ecommerce.reporting.infrastructure.persistence;

import com.company.ecommerce.reporting.application.ProductSalesRow;
import com.company.ecommerce.reporting.domain.ProductSalesFact;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence and aggregation for {@link ProductSalesFact} rows. */
public interface ProductSalesFactRepository extends JpaRepository<ProductSalesFact, UUID> {

    boolean existsByOrderId(UUID orderId);

    @Query(
            value =
                    """
                    SELECT new com.company.ecommerce.reporting.application.ProductSalesRow(
                        f.productId,
                        COALESCE(SUM(f.quantity), 0),
                        COUNT(DISTINCT f.orderId))
                    FROM ProductSalesFact f
                    WHERE f.orderDate BETWEEN :from AND :to
                    GROUP BY f.productId
                    ORDER BY SUM(f.quantity) DESC, f.productId
                    """,
            countQuery =
                    """
                    SELECT COUNT(DISTINCT f.productId)
                    FROM ProductSalesFact f
                    WHERE f.orderDate BETWEEN :from AND :to
                    """)
    Page<ProductSalesRow> topProducts(
            @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);
}
