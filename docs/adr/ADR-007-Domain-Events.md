# ADR-007 Domain Events for Module Communication

Status: Accepted

## Context

Modules should remain loosely coupled while still reacting to business actions.

## Decision

Use Spring Modulith domain events as the primary communication mechanism between modules.

## Alternatives Considered

* Direct service calls
* Shared repositories

## Consequences

### Positive

* Lower coupling
* Better modularity
* Easier future extraction to microservices

### Negative

* Event flow becomes harder to trace
* Event ordering considerations

## Amendment (Phase 4 — Order): events vs. orchestration at acyclic boundaries

Spring Modulith forbids cyclic module dependencies (`ApplicationModules.verify()`), and an event
*consumer* depends on the *publisher's* event types. The `order` module must read `cart`, `coupon`
and `user` at checkout (so `order → cart/coupon/user`), and `cart → inventory` already exists.
Therefore none of `cart`, `coupon` or `inventory` may depend back on `order` — i.e. they cannot
consume `OrderCreatedEvent`/`OrderCancelledEvent` without forming a cycle (e.g.
`order → cart → inventory → order`).

Decision: keep events the default, but where a cycle would otherwise form, the upstream module
**orchestrates** its dependencies through their `spi` named interfaces instead of those modules
consuming order events:

* `order` reserves/releases stock, clears the cart and records coupon usage by calling
  `inventory.spi.StockReservations`, `cart.spi.CartMaintenance` and `coupon.spi.CouponRedemption`.
* To keep these out of the place-order transaction, `order` publishes its own
  `OrderCreatedEvent`/`OrderCancelledEvent` and consumes them on **in-module**
  `@ApplicationModuleListener`s, so each side effect runs after commit in its own transaction.
* Genuinely downstream modules that `order` does **not** read (payment, notification, audit, and
  review/reporting on `OrderCompletedEvent`) still consume order events directly (choreography).

This preserves loose coupling and per-module transactions while keeping the module graph acyclic.

## Related Documents

* MODULES.md (see the ORDER module note)
* ARCHITECTURE.md (§11 implementation note)
