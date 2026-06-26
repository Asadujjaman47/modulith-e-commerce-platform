/**
 * Reporting module: read-only sales and product reports built from order events. (Phase 7)
 *
 * <p>Owns no business aggregate. It consumes {@code OrderCreatedEvent} (the only order event carrying
 * line items and totals) via the order {@code events} named interface and records immutable per-order
 * facts ({@code SalesFact}, {@code ProductSalesFact}); sales and product reports are computed by
 * aggregating those facts at query time. Fact writes are idempotent (guarded by a unique
 * {@code order_id}), so the at-least-once event delivery cannot double count. Exposes admin-only report
 * endpoints under {@code /api/v1/admin/reports}. References orders, customers and products by id/value
 * only (no cross-module FKs) and depends on no other module.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Reporting")
package com.company.ecommerce.reporting;