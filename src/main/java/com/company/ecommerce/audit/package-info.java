/**
 * Audit module: append-only activity trail built from all business events. (Phase 7)
 *
 * <p>Consumes every published business event (auth, user, catalog, inventory, coupon, order, payment,
 * shipment, review, notification) via their {@code events} named interfaces on post-commit
 * {@code @ApplicationModuleListener}s and records one immutable {@code AuditLog} row each. Exposes
 * admin-only search ({@code /api/v1/admin/audit-logs}) and a per-user activity timeline. References
 * affected entities and acting customers by id value only (no cross-module FKs); no other module
 * depends on audit, so the trail stays a pure sink and the module graph stays acyclic.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Audit")
package com.company.ecommerce.audit;