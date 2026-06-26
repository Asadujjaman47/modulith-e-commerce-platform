package com.company.ecommerce.reporting.application;

import com.company.ecommerce.common.api.PageResponse;
import com.company.ecommerce.common.exception.BusinessException;
import com.company.ecommerce.reporting.api.dto.ProductSalesResponse;
import com.company.ecommerce.reporting.infrastructure.persistence.ProductSalesFactRepository;
import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds a product report: top products by units sold over a date window, paginated. */
@Service
@RequiredArgsConstructor
public class ProductReportUseCase {

    private final ProductSalesFactRepository productSalesFactRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductSalesResponse> report(LocalDate from, LocalDate to, Pageable pageable) {
        if (from.isAfter(to)) {
            throw new BusinessException("Report start date must not be after the end date");
        }

        // Ordering is fixed by the aggregation query (units sold desc); only page/size are honoured.
        Pageable paging = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        return PageResponse.from(
                productSalesFactRepository
                        .topProducts(from, to, paging)
                        .map(
                                row ->
                                        new ProductSalesResponse(
                                                row.productId(), row.unitsSold(), row.orderCount())));
    }
}
