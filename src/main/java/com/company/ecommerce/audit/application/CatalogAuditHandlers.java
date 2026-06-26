package com.company.ecommerce.audit.application;

import com.company.ecommerce.audit.domain.AuditCategory;
import com.company.ecommerce.catalog.domain.event.ProductCreatedEvent;
import com.company.ecommerce.catalog.domain.event.ProductDeletedEvent;
import com.company.ecommerce.catalog.domain.event.ProductUpdatedEvent;
import com.company.ecommerce.coupon.domain.event.CouponAppliedEvent;
import com.company.ecommerce.coupon.domain.event.CouponExpiredEvent;
import com.company.ecommerce.inventory.domain.event.StockReleasedEvent;
import com.company.ecommerce.inventory.domain.event.StockReservedEvent;
import com.company.ecommerce.inventory.domain.event.StockUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Records audit entries for catalog, inventory and coupon events. */
@Component
@RequiredArgsConstructor
public class CatalogAuditHandlers {

    private final AuditLogWriter audit;

    @ApplicationModuleListener
    public void on(ProductCreatedEvent event) {
        audit.record(
                AuditCategory.CATALOG,
                "ProductCreated",
                "CREATE",
                "Product",
                event.productId(),
                null,
                "Product created: " + event.sku());
    }

    @ApplicationModuleListener
    public void on(ProductUpdatedEvent event) {
        audit.record(
                AuditCategory.CATALOG,
                "ProductUpdated",
                "UPDATE",
                "Product",
                event.productId(),
                null,
                "Product updated");
    }

    @ApplicationModuleListener
    public void on(ProductDeletedEvent event) {
        audit.record(
                AuditCategory.CATALOG,
                "ProductDeleted",
                "DELETE",
                "Product",
                event.productId(),
                null,
                "Product deleted");
    }

    @ApplicationModuleListener
    public void on(StockReservedEvent event) {
        audit.record(
                AuditCategory.INVENTORY,
                "StockReserved",
                "RESERVE",
                "Inventory",
                event.productId(),
                null,
                "Reserved %d unit(s); reservation %s"
                        .formatted(event.quantity(), event.reservationId()));
    }

    @ApplicationModuleListener
    public void on(StockReleasedEvent event) {
        audit.record(
                AuditCategory.INVENTORY,
                "StockReleased",
                "RELEASE",
                "Inventory",
                event.productId(),
                null,
                "Released %d unit(s); reservation %s"
                        .formatted(event.quantity(), event.reservationId()));
    }

    @ApplicationModuleListener
    public void on(StockUpdatedEvent event) {
        audit.record(
                AuditCategory.INVENTORY,
                "StockUpdated",
                "UPDATE",
                "Inventory",
                event.productId(),
                null,
                "Stock updated: onHand=%d available=%d"
                        .formatted(event.quantityOnHand(), event.quantityAvailable()));
    }

    @ApplicationModuleListener
    public void on(CouponAppliedEvent event) {
        audit.record(
                AuditCategory.COUPON,
                "CouponApplied",
                "APPLY",
                "Coupon",
                event.couponId(),
                event.customerId(),
                "Coupon %s applied; discount %s".formatted(event.code(), event.discountAmount()));
    }

    @ApplicationModuleListener
    public void on(CouponExpiredEvent event) {
        audit.record(
                AuditCategory.COUPON,
                "CouponExpired",
                "EXPIRE",
                "Coupon",
                event.couponId(),
                null,
                "Coupon %s expired".formatted(event.code()));
    }
}
