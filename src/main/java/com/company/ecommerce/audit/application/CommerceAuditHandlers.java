package com.company.ecommerce.audit.application;

import com.company.ecommerce.audit.domain.AuditCategory;
import com.company.ecommerce.order.domain.event.OrderCancelledEvent;
import com.company.ecommerce.order.domain.event.OrderCompletedEvent;
import com.company.ecommerce.order.domain.event.OrderCreatedEvent;
import com.company.ecommerce.payment.domain.event.PaymentCompletedEvent;
import com.company.ecommerce.payment.domain.event.PaymentFailedEvent;
import com.company.ecommerce.payment.domain.event.PaymentRefundedEvent;
import com.company.ecommerce.shipment.domain.event.ShipmentCreatedEvent;
import com.company.ecommerce.shipment.domain.event.ShipmentDeliveredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Records audit entries for order, payment and shipment events. */
@Component
@RequiredArgsConstructor
public class CommerceAuditHandlers {

    private final AuditLogWriter audit;

    @ApplicationModuleListener
    public void on(OrderCreatedEvent event) {
        audit.record(
                AuditCategory.ORDER,
                "OrderCreated",
                "CREATE",
                "Order",
                event.orderId(),
                event.customerId(),
                "Order %s placed; total %s".formatted(event.orderNumber(), event.totalAmount()));
    }

    @ApplicationModuleListener
    public void on(OrderCancelledEvent event) {
        audit.record(
                AuditCategory.ORDER,
                "OrderCancelled",
                "CANCEL",
                "Order",
                event.orderId(),
                event.customerId(),
                "Order cancelled");
    }

    @ApplicationModuleListener
    public void on(OrderCompletedEvent event) {
        audit.record(
                AuditCategory.ORDER,
                "OrderCompleted",
                "COMPLETE",
                "Order",
                event.orderId(),
                event.customerId(),
                "Order completed (delivered)");
    }

    @ApplicationModuleListener
    public void on(PaymentCompletedEvent event) {
        audit.record(
                AuditCategory.PAYMENT,
                "PaymentCompleted",
                "PAYMENT",
                "Payment",
                event.paymentId(),
                event.customerId(),
                "Payment of %s %s succeeded for order %s"
                        .formatted(event.amount(), event.currency(), event.orderId()));
    }

    @ApplicationModuleListener
    public void on(PaymentFailedEvent event) {
        audit.record(
                AuditCategory.PAYMENT,
                "PaymentFailed",
                "PAYMENT",
                "Payment",
                event.paymentId(),
                event.customerId(),
                "Payment failed for order %s: %s".formatted(event.orderId(), event.reason()));
    }

    @ApplicationModuleListener
    public void on(PaymentRefundedEvent event) {
        audit.record(
                AuditCategory.PAYMENT,
                "PaymentRefunded",
                "REFUND",
                "Payment",
                event.paymentId(),
                event.customerId(),
                "Payment of %s refunded for order %s".formatted(event.amount(), event.orderId()));
    }

    @ApplicationModuleListener
    public void on(ShipmentCreatedEvent event) {
        audit.record(
                AuditCategory.SHIPMENT,
                "ShipmentCreated",
                "CREATE",
                "Shipment",
                event.shipmentId(),
                event.customerId(),
                "Shipment created (%s via %s) for order %s"
                        .formatted(event.trackingNumber(), event.carrier(), event.orderId()));
    }

    @ApplicationModuleListener
    public void on(ShipmentDeliveredEvent event) {
        audit.record(
                AuditCategory.SHIPMENT,
                "ShipmentDelivered",
                "DELIVER",
                "Shipment",
                event.shipmentId(),
                event.customerId(),
                "Shipment delivered for order %s".formatted(event.orderId()));
    }
}
