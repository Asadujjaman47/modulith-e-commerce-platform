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
read (payment, notification, audit, and review/reporting on `OrderCompletedEvent`) consume the order
events directly.

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
* Public endpoints: `/api/v1/auth/{register,login,refresh}`, `/actuator/**`, Swagger. Everything
  else requires authentication. `@EnableMethodSecurity` enables role checks.
* The JWT signing secret (`app.jwt.secret`) has a dev-only fallback for local runs; the Docker/deploy
  path requires `JWT_SECRET` to be set (fails fast otherwise).
* 401/403 responses use the standard `ErrorResponse` envelope.

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

Redis Usage

Cart Cache

Product Cache

Coupon Cache

Session Cache

---

TTL

Cart

24 hours

Product

1 hour

Coupon

30 minutes

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