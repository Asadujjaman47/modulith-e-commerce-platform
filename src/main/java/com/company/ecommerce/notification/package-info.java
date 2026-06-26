/**
 * Notification module: email notifications driven by events. (Phase 6)
 *
 * <p>An event-driven module with no public API. Consumes the published {@code events} named interfaces
 * of {@code auth} ({@code UserRegisteredEvent}), {@code order} ({@code OrderCreatedEvent}),
 * {@code payment} ({@code PaymentCompletedEvent}) and {@code shipment}
 * ({@code ShipmentCreatedEvent}/{@code ShipmentDeliveredEvent}), and for each sends an email and
 * appends a {@code NotificationLog}. Publishes {@code NotificationSentEvent}.
 *
 * <p>The module depends on no other module structurally: recipient contact details are kept in a local
 * replica ({@code NotificationRecipient}) seeded from {@code UserRegisteredEvent}, so later events —
 * whose {@code customerId} equals the auth {@code userId} — can be addressed without calling the
 * {@code user} module. Delivery failures are recorded as {@code FAILED} logs and never propagate, so a
 * mail outage cannot disrupt the business flow that triggered the notification.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Notification")
package com.company.ecommerce.notification;
