# ARCHITECTURE.md

# E-Commerce Platform Architecture

Version: 1.0

Architecture Style: Spring Modulith (Modular Monolith)

Language: Java 21

Framework: Spring Boot + Spring Modulith

Database: PostgreSQL

Cache: Redis

Deployment: Docker

---

# 1. Architecture Overview

The application is implemented as a Modular Monolith.

The system is deployed as a single Spring Boot application but internally divided into independent business modules.

Goals:

* High maintainability
* Strong module boundaries
* Low coupling
* High cohesion
* Event-driven communication
* Future microservice extraction capability

---

# 2. System Context

Customer

↓

Web / Mobile Client

↓

REST API

↓

Spring Modulith Application

↓

PostgreSQL

↓

Redis

↓

External Services

* Email Provider
* SMS Provider
* Payment Gateway

---

# 3. Module Map

ecommerce

├── auth

├── user

├── catalog

├── inventory

├── cart

├── coupon

├── order

├── payment

├── shipment

├── notification

├── review

├── reporting

├── audit

├── common

└── config

---

# 4. Module Dependency Rules

Allowed Dependencies

auth
↓
user

cart
↓
catalog

order
↓
cart

order
↓
coupon

payment
↓
order

shipment
↓
order

notification
↓
events only

audit
↓
events only

reporting
↓
events only

Forbidden Dependencies

catalog -> order

catalog -> payment

inventory -> payment

shipment -> payment

review -> payment

notification -> direct service calls

audit -> direct service calls

Modules should communicate through events whenever possible.

---

# 5. Dependency Diagram

```
                +-------------+
                |    AUTH     |
                +-------------+
                       |
                       v
                +-------------+
                |    USER     |
                +-------------+
```
```

+-------------+     +-------------+
|  CATALOG    |<--->| INVENTORY   |
+-------------+     +-------------+

```
        |
        v
```

+-------------+
|    CART     |
+-------------+

```
        |
        v
```

+-------------+
|    ORDER    |
+-------------+

```
        |
        +----------------+
        |                |
        v                v
```

+-------------+   +-------------+
|  PAYMENT    |   | SHIPMENT    |
+-------------+   +-------------+

```
        |
        v
```

+-------------+
|NOTIFICATION |
+-------------+

```
        |
        v
```

+-------------+
|   AUDIT     |
+-------------+
```

---

# 6. Aggregate Design

Each module owns its aggregates.

No aggregate may be modified by another module.

---

## Auth Aggregate

UserCredential

RefreshToken

---

## User Aggregate

Customer

Address

---

## Catalog Aggregate

Product

Category

Brand

ProductImage

---

## Inventory Aggregate

Inventory

StockReservation

InventoryTransaction

---

## Cart Aggregate

Cart

CartItem

---

## Coupon Aggregate

Coupon

CouponUsage

---

## Order Aggregate

Order

OrderItem

OrderAddress

---

## Payment Aggregate

Payment

PaymentTransaction

---

## Shipment Aggregate

Shipment

ShipmentTracking

---

## Review Aggregate

Review

Rating

---

# 7. Aggregate Ownership

Only owning module can modify aggregate.

Example:

Order Aggregate

Owner:

order module

Allowed:

order module

Forbidden:

payment module

shipment module

notification module

audit module

These modules may only react to events.

---

# 8. Event Architecture

Primary communication mechanism:

Domain Events

Pattern:

Module A

↓

Publish Event

↓

Module B

↓

Consume Event

---

# 9. Core Events

UserRegisteredEvent

ProductCreatedEvent

ProductUpdatedEvent

StockReservedEvent

StockReleasedEvent

OrderCreatedEvent

OrderCancelledEvent

PaymentCompletedEvent

PaymentFailedEvent

ShipmentCreatedEvent

ShipmentDeliveredEvent

ReviewCreatedEvent

CouponAppliedEvent

---

# 10. Checkout Flow

Customer

↓

Add Product To Cart

↓

Checkout

↓

Order Created

↓

Inventory Reserved

↓

Payment Processed

↓

Shipment Created

↓

Notification Sent

↓

Audit Logged

---

# 11. Event Flow

OrderCreatedEvent

↓

Inventory Module

↓

Reserve Stock

↓

StockReservedEvent

↓

Payment Module

↓

Create Payment

↓

PaymentCompletedEvent

↓

Shipment Module

↓

Create Shipment

↓

ShipmentCreatedEvent

↓

Notification Module

↓

Send Email

↓

Audit Module

↓

Store Audit Record

---

Implementation note (Phase 4): the diagrams above describe the conceptual business flow. As built,
the **order** module orchestrates the modules it depends on (cart, coupon, inventory) — it reserves
stock, clears the cart and records coupon usage through their `spi` commands rather than those
modules consuming `OrderCreatedEvent`. This keeps the module dependency graph acyclic (order reads
cart, and `cart -> inventory`, so neither may depend back on order). Each side effect still runs
**after** the place-order transaction commits and in **its own** transaction, via in-module
`@ApplicationModuleListener`s on the order events. Genuinely downstream modules that order does not
read (payment, notification, audit, reporting and review) consume the order events directly.

Implementation note (Phase 5): **payment** and **shipment** both depend on **order** (never the
reverse). Payment consumes `OrderCreatedEvent` to create a PENDING intent; on a successful charge it
publishes `PaymentCompletedEvent` and — via a post-commit in-module `@ApplicationModuleListener` —
marks the order `PAID` through the new order `spi` (`OrderLifecycle`). Shipment consumes
`PaymentCompletedEvent` to auto-create a shipment and likewise pushes the order to `PROCESSING`
(on creation) and `DELIVERED` (on delivery) through the same `spi`. Order does **not** consume
payment/shipment events (that would form a cycle), so all order-status advancement is pushed in
through `OrderLifecycle`, whose transitions are idempotent "ensure at least" walks of the happy path —
tolerant of the unordered async events that drive them.

Implementation note (Phase 7): **reporting** and **audit** are read-only sinks — nothing depends on
them, so they never form a cycle. **Reporting** consumes `OrderCreatedEvent` (the only order event
carrying both line items and monetary totals) and records immutable per-order/per-line facts;
sales/product reports aggregate those facts on demand. Fact writes are idempotent (unique `order_id`),
so at-least-once delivery cannot double count. **Audit** consumes **every** published business event
via the modules' `events` named interfaces and appends one immutable `AuditLog` row each. Both run on
post-commit `@ApplicationModuleListener`s and reference other modules by id value only.

---

# 12. Transaction Boundaries

One transaction per module operation.

Good:

OrderService

@Transactional

createOrder()

Bad:

OrderService

@Transactional

createOrder()

↓

InventoryService

↓

PaymentService

↓

ShipmentService

inside same transaction

Avoid distributed transaction behavior.

---

# 13. API Architecture

External Layer

Controllers

↓

Application Layer

Use Cases

↓

Domain Layer

Business Rules

↓

Infrastructure Layer

Persistence

External APIs

---

# 14. DTO Rules

Controllers never expose:

Entity

Aggregate

Repository Objects

Controllers expose only:

Request DTO

Response DTO

Use MapStruct for mapping.

---

# 15. Security Architecture

Authentication

JWT Access Token

JWT Refresh Token

Authorization

ROLE_ADMIN

ROLE_CUSTOMER

---

Admin APIs

/api/v1/admin/**

Customer APIs

/api/v1/**

---

Implementation (Phase 1)

* Security is owned by the `auth` module (`auth.infrastructure.security.SecurityConfig`), not `config`.
* Stateless `SecurityFilterChain`; `JwtAuthenticationFilter` validates the `Authorization: Bearer`
  header and sets an `AuthenticatedUser` principal whose name is the user id.
* Access tokens: HMAC-SHA256 JWT, 15-minute TTL, carrying `sub` (user id), `email`, `role`.
* Refresh tokens: opaque random strings, 7-day TTL, **SHA-256 hashed and stored in PostgreSQL**
  (`refresh_tokens`). Refresh rotates the token (old one revoked); logout revokes it.
* Public endpoints: `/api/v1/auth/{register,login,refresh}`, `/actuator/health` + `/actuator/info`,
  Swagger. Everything else requires authentication. `@EnableMethodSecurity` enables role checks.
* The JWT signing secret (`app.jwt.secret`) has a dev-only fallback for local runs; the Docker/deploy
  path requires `JWT_SECRET` to be set (fails fast otherwise).
* 401/403 responses use the standard `ErrorResponse` envelope.

Production hardening (Phase 9):

* **Security headers** on every response: `X-Content-Type-Options=nosniff`, `X-Frame-Options=DENY`,
  `Referrer-Policy=strict-origin-when-cross-origin`, HSTS (over HTTPS).
* **CORS** bound from `app.cors.*` (`CorsProperties`) and applied to `/api/**`; exact origins only,
  no default origin in the `prod` profile.
* **Actuator lockdown**: only `health`/`info` are public; `prometheus`, `metrics`, `modulith`, etc.
  require `ROLE_ADMIN`.
* **`prod` profile**: removes the JWT-secret fallback (fail-fast), disables Swagger, hides health
  detail, silences SQL logging.
* **Rate limiting** (Bucket4j over Redis) on `/api/**` — see [RATE_LIMITING.md](RATE_LIMITING.md).
* Full reference: [SECURITY.md](SECURITY.md).

---

# 16. Persistence Architecture

Database

PostgreSQL

Schema Management

Flyway

Naming Convention

V1__init.sql        (foundation: pgcrypto, event_publication)

V2__auth.sql        (auth_users, refresh_tokens)

V3__user.sql        (customers, customer_addresses)

V4__catalog.sql     (categories, brands, products, product_images)

V5__inventory.sql   (inventory, stock_reservations, inventory_transactions)

Existing migrations are immutable; schema changes always add a new versioned migration.

---

# 17. Redis Architecture

Redis backs two subsystems (PostgreSQL remains the source of truth):

1. **Spring Cache** (`config.CacheConfig`, `@EnableCaching` + `RedisCacheManager`)
2. **Rate-limit buckets** (Bucket4j `ProxyManager`, see [RATE_LIMITING.md](RATE_LIMITING.md))

Cache catalog (read-through, JSON values, key prefix `ecommerce:cache:`):

| Cache | Contents | TTL | Eviction |
| ----- | -------- | --- | -------- |
| `products` | product-by-id | 15 min | on product update/delete |
| `categoryList` | full category list | 30 min | on any category write |
| `brandList` | full brand list | 30 min | on any brand write |

Coupon-by-code is intentionally **not** cached: that read lazily expires/deactivates coupons and
publishes events, so it is not a pure read. Cart stays in PostgreSQL (write-heavy, source of truth).

Resilience: a `LoggingCacheErrorHandler` downgrades any Redis cache failure to a database read, and the
Lettuce client is pooled (`commons-pool2`) with short command/connect timeouts so a slow/unreachable
Redis fails fast rather than hanging requests.

---

# 18. Search Architecture

Phase 1

Database Search

JPA Specification

Pagination

Sorting

Filtering

Phase 2

Elasticsearch

Optional

Not required initially

---

# 19. Observability Architecture

Spring Boot Actuator

↓

Micrometer

↓

Prometheus

↓

Grafana

Metrics

* JVM
* Memory
* CPU
* Request Count
* Response Time
* Database Connections
* Redis Connections

Implemented in Phase 8. Distributed tracing (Micrometer Tracing → Brave → Zipkin) and custom
business metrics (orders, payments, shipments, reviews, registrations) are wired alongside the
technical metrics; the business counters are driven from published domain events in the
cross-cutting `config` module, so no business module depends on Micrometer. Grafana dashboards and
the Prometheus datasource are auto-provisioned, and Prometheus loads alert rules. See
`docs/OBSERVABILITY.md` (reference) and `docs/OBSERVABILITY_RUNBOOK.md` (operations).

---

# 20. Logging Architecture

Structured Logging

Required Fields

timestamp

traceId

module

action

entityId

userId

Example

Order Created

Payment Completed

Shipment Delivered

---

# 21. Testing Architecture

Unit Tests

Application Service Tests

Domain Tests

Module Tests

Integration Tests

Testcontainers

Module Verification Tests

---

# 22. Docker Architecture

Services

postgres

redis

mailpit

app

prometheus

grafana

Network

ecommerce-network

Volume

postgres-data

grafana-data

---

# 23. Future Expansion

Potential Microservice Candidates

order

payment

shipment

notification

Only after proven scaling requirements.

Current target remains:

Modular Monolith.

---

# 24. Architectural Principles

1. Business modules first.

2. Package-by-feature.

3. Events over direct coupling.

4. One aggregate owner.

5. No shared business logic.

6. DTOs at boundaries.

7. Flyway for schema evolution.

8. Constructor injection only.

9. Test critical flows.

10. Docker-first deployment.

---

# 25. Architectural Decision Records (ADR)

Every major decision must be documented.

Examples:

ADR-001-Modular-Monolith.md

ADR-002-PostgreSQL.md

ADR-003-UUID-Identifiers.md

ADR-004-JWT-Authentication.md

ADR-005-Flyway-Migrations.md

ADR-006-Redis-Caching.md

ADR-007-Domain-Events.md

ADR-008-Docker-Deployment.md

ADR-009-MapStruct.md

ADR-010-Testcontainers.md