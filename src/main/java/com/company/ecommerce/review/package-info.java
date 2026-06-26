/**
 * Review module: product reviews and ratings. (Phase 6)
 *
 * <p>Lets purchase-eligible customers post a star rating (1–5) and comment for a product, maintains an
 * aggregate {@code ProductRating} per product, and lets admins moderate (hide/restore) reviews. Reads
 * the product (active flag) via the catalog {@code spi} and the author's display name via the user
 * {@code spi}, snapshotting the name on the review. The purchase gate is enforced against a local
 * eligibility replica seeded from {@code OrderCompletedEvent} (consumed via the order {@code events}
 * named interface), so review never depends on the order module. Publishes {@code ReviewCreatedEvent}.
 * References products and customers by id/value only (no cross-module FKs).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Review")
package com.company.ecommerce.review;
