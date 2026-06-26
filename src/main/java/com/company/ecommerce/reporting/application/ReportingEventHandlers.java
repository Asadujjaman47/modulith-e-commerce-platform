package com.company.ecommerce.reporting.application;

import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.order.domain.event.OrderLine;
import com.company.ecommerce.reporting.domain.ProductSalesFact;
import com.company.ecommerce.reporting.domain.SalesFact;
import com.company.ecommerce.reporting.infrastructure.persistence.ProductSalesFactRepository;
import com.company.ecommerce.reporting.infrastructure.persistence.SalesFactRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Builds the reporting projections from order events. Runs after the order transaction commits, in its
 * own transaction ({@code @ApplicationModuleListener}).
 *
 * <p>{@code OrderCreatedEvent} is the only order event carrying both line items and monetary totals, so
 * it drives both the sales and the product projections. Recording is idempotent — a replayed event for
 * an order already on file is ignored — so the at-least-once delivery guarantee cannot double count.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportingEventHandlers {

    private final SalesFactRepository salesFactRepository;
    private final ProductSalesFactRepository productSalesFactRepository;

    @ApplicationModuleListener
    public void on(OrderCreatedEvent event) {
        if (salesFactRepository.existsByOrderId(event.orderId())) {
            log.debug("Sales fact already recorded for order {}; skipping.", event.orderId());
            return;
        }

        LocalDate orderDate = LocalDate.now(ZoneOffset.UTC);
        int itemCount = event.lines().stream().mapToInt(OrderLine::quantity).sum();

        salesFactRepository.save(
                SalesFact.record(
                        event.orderId(),
                        event.customerId(),
                        orderDate,
                        event.totalAmount(),
                        event.discountAmount(),
                        itemCount));

        List<ProductSalesFact> productFacts =
                event.lines().stream()
                        .map(
                                line ->
                                        ProductSalesFact.record(
                                                event.orderId(),
                                                line.productId(),
                                                line.quantity(),
                                                orderDate))
                        .toList();
        productSalesFactRepository.saveAll(productFacts);

        log.info(
                "Recorded sales projection for order {} ({} lines, {} units).",
                event.orderId(),
                productFacts.size(),
                itemCount);
    }
}
