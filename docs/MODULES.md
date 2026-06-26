# MODULES.md

# E-Commerce Platform Module Specification

Version: 1.0

Architecture: Spring Modulith

---

# Purpose

This document defines:

* Module responsibilities
* Aggregate ownership
* Public APIs
* Published events
* Consumed events
* Database ownership
* Allowed dependencies

Rules in this document take precedence over implementation convenience.

A feature must always belong to exactly one module.

---

# Module Catalog

| Module       | Purpose                        |
| ------------ | ------------------------------ |
| auth         | Authentication & authorization |
| user         | Customer profile management    |
| catalog      | Product catalog                |
| inventory    | Stock management               |
| cart         | Shopping cart                  |
| coupon       | Promotions & discounts         |
| order        | Order lifecycle                |
| payment      | Payment processing             |
| shipment     | Delivery management            |
| notification | Email/SMS notifications        |
| review       | Product reviews                |
| reporting    | Reporting & analytics          |
| audit        | Audit trail                    |

---

# AUTH MODULE

Status: Implemented (Phase 1)

Package

com.company.ecommerce.auth

---

Responsibilities

* Register user
* Login
* Logout
* Refresh token
* Password reset
* JWT generation
* JWT validation

---

Aggregate Roots

UserCredential

RefreshToken

---

Database Tables

auth_users

refresh_tokens

---

Public APIs

RegisterUserUseCase

LoginUseCase

RefreshTokenUseCase

LogoutUseCase

---

Published Events

UserRegisteredEvent

UserLoggedInEvent

---

Consumed Events

None

---

Allowed Dependencies

user

---

Forbidden Dependencies

catalog

inventory

cart

order

payment

shipment

review

---

# USER MODULE

Status: Implemented (Phase 1)

Package

com.company.ecommerce.user

---

Responsibilities

* Customer profile
* Customer addresses
* Account settings

---

Aggregate Roots

Customer

Address

---

Database Tables

customers

customer_addresses

---

Public APIs

CreateCustomerUseCase

UpdateCustomerUseCase

GetCustomerUseCase

UserQuery (read-only `spi`: resolve a customer's address for other modules — used by `order`)

---

Published Events

CustomerCreatedEvent

CustomerUpdatedEvent

AddressAddedEvent

---

Consumed Events

UserRegisteredEvent

---

Allowed Dependencies

None

---

# CATALOG MODULE

Status: Implemented (Phase 2)

Package

com.company.ecommerce.catalog

---

Responsibilities

* Product management
* Categories
* Brands
* Product images
* Product search

---

Aggregate Roots

Product

Category

Brand

(ProductImage is a child entity of the Product aggregate, not an aggregate root.)

---

Database Tables

products

categories

brands

product_images

---

Public APIs

CreateProductUseCase

UpdateProductUseCase

GetProductUseCase

DeleteProductUseCase

SearchProductUseCase

ManageCategoriesUseCase

ManageBrandsUseCase

CatalogQuery (read-only `spi` named interface: ProductView lookup for other modules)

---

Published Events

ProductCreatedEvent

ProductUpdatedEvent

ProductDeletedEvent

---

Consumed Events

None

---

Allowed Dependencies

None

---

# INVENTORY MODULE

Status: Implemented (Phase 2)

Package

com.company.ecommerce.inventory

---

Responsibilities

* Stock tracking
* Stock reservation
* Stock release

---

Aggregate Roots

Inventory

StockReservation

InventoryTransaction

---

Database Tables

inventory

stock_reservations

inventory_transactions

---

Public APIs

ReserveStockUseCase

ReleaseStockUseCase

UpdateStockUseCase

GetStockUseCase

InventoryQuery (read-only `spi`: available-quantity lookup for other modules)

StockReservations (`spi` command: reserve / release-by-reference — used by `order`)

---

Published Events

StockReservedEvent

StockReleasedEvent

StockUpdatedEvent

---

Consumed Events

ProductCreatedEvent (catalog) — seeds a zero-stock inventory record

(`order` reserves/releases stock by calling the StockReservations `spi`; inventory does not consume
order events — that would form a cycle via `cart -> inventory`.)

---

Allowed Dependencies

catalog

---

# CART MODULE

Status: Implemented (Phase 3)

Package

com.company.ecommerce.cart

---

Responsibilities

* Customer shopping cart
* Cart item management

---

Aggregate Roots

Cart

CartItem

---

Database Tables

carts

cart_items

---

Public APIs

GetCartUseCase

AddToCartUseCase

UpdateCartItemUseCase

RemoveCartItemUseCase

CartQuery (read-only `spi`: read the cart at checkout — used by `order`)

CartMaintenance (`spi` command: clear the cart — used by `order` after an order is placed)

---

Published Events

None (cart clearing at checkout is driven by `order` via the CartMaintenance `spi`)

---

Consumed Events

ProductUpdatedEvent — refreshes cart-item price/name snapshots

StockUpdatedEvent (auto-trim deferred)

---

Allowed Dependencies

catalog (read via the catalog `spi` named interface: CatalogQuery + ProductView)

inventory (read via the inventory `spi` named interface: InventoryQuery)

---

# COUPON MODULE

Status: Implemented (Phase 3)

Package

com.company.ecommerce.coupon

---

Responsibilities

* Coupon creation
* Discount validation
* Promotion management

---

Aggregate Roots

Coupon

CouponUsage

---

Database Tables

coupons

coupon_usages

---

Public APIs

CreateCouponUseCase

ValidateCouponUseCase

ApplyCouponUseCase

CouponQuery (read-only `spi`: quote a discount — used by `order` at checkout)

CouponRedemption (`spi` command: record usage linked to a placed order — used by `order`)

---

Published Events

CouponAppliedEvent

CouponExpiredEvent (published on lazy expiry during validate/apply)

---

Consumed Events

None. `order` records per-order usage by calling the CouponRedemption `spi` (coupon does not depend
on order — that would form a cycle).

---

Allowed Dependencies

None (validation/apply/quote operate on an order amount supplied by the caller)

---

# ORDER MODULE

Status: Implemented (Phase 4)

Package

com.company.ecommerce.order

---

Responsibilities

* Order creation
* Order cancellation
* Order history
* Order status tracking

---

Aggregate Roots

Order

(OrderItem and OrderAddress are child entities of the Order aggregate, not aggregate roots.)

---

Database Tables

orders

order_items

order_addresses

---

Public APIs

PlaceOrderUseCase

CancelOrderUseCase

GetOrderUseCase

ListOrdersUseCase

UpdateOrderStatusUseCase (admin status lifecycle)

OrderQuery (read-only `spi`: read an order — used by `payment` and `shipment`)

OrderLifecycle (`spi` command: "ensure at least" PAID/PROCESSING/DELIVERED transitions, walking the
happy path — used by `payment` and `shipment` to advance the order without a dependency cycle)

---

Published Events

OrderCreatedEvent

OrderCancelledEvent

OrderCompletedEvent (published on delivery)

---

Consumed Events

None from other modules. Order is the orchestrator: it reads cart/user/coupon and pre-checks
inventory via their `spi` named interfaces, then drives the downstream side effects (reserve/release
stock, clear cart, record coupon usage) via those modules' `spi` commands. To keep these out of the
placing transaction, order consumes its own `OrderCreatedEvent`/`OrderCancelledEvent` on in-module
`@ApplicationModuleListener`s so each side effect runs after commit, in its own transaction.

> Note: the earlier design had order consume `CartCheckedOutEvent`/`CouponAppliedEvent` and have
> inventory/coupon consume `OrderCreatedEvent`. That forms a dependency cycle (order reads cart, and
> `cart -> inventory`, so neither inventory nor cart/coupon may depend back on order). The
> orchestration model above replaces it and keeps the module graph acyclic.

---

Allowed Dependencies

cart (spi: CartQuery read + CartMaintenance command)

coupon (spi: CouponQuery quote + CouponRedemption command)

user (spi: UserQuery — resolve the shipping address)

inventory (spi: InventoryQuery pre-check + StockReservations reserve/release)

---

# PAYMENT MODULE

Status: Implemented (Phase 5)

Package

com.company.ecommerce.payment

---

Responsibilities

* Payment processing
* Payment verification
* Refund management

---

Aggregate Roots

Payment

PaymentTransaction

---

Database Tables

payments

payment_transactions

---

Public APIs

CreatePaymentUseCase (process a charge through the pluggable PaymentGateway)

GetPaymentUseCase (payment status, by id — owner-scoped or admin)

ListPaymentsUseCase (payment history, paginated, optional order filter)

RefundPaymentUseCase (admin)

---

Published Events

PaymentCompletedEvent

PaymentFailedEvent

PaymentRefundedEvent

---

Consumed Events

OrderCreatedEvent (order) — creates a PENDING payment intent for the placed order

---

Allowed Dependencies

order (spi: OrderQuery read + OrderLifecycle command — validate the payable amount and, on a
successful charge, mark the order PAID after commit via an in-module @ApplicationModuleListener)

> Note: payment does not consume payment/shipment events back into order. order never depends on
> payment, so marking the order PAID is *pushed* through the order `spi` to keep the graph acyclic.

---

# SHIPMENT MODULE

Status: Implemented (Phase 5)

Package

com.company.ecommerce.shipment

---

Responsibilities

* Shipment creation
* Shipment tracking
* Delivery confirmation

---

Aggregate Roots

Shipment

(TrackingRecord is a child entity of the Shipment aggregate, not an aggregate root. DeliveryAddress is
an embedded value object snapshotting the order's delivery address.)

---

Database Tables

shipments

tracking_records

---

Public APIs

CreateShipmentUseCase (admin manual create + event-driven createForOrder; idempotent per order)

TrackShipmentUseCase (shipment + tracking history — owner-scoped or admin)

UpdateShipmentStatusUseCase (admin status progression, appends tracking records)

MarkDeliveredUseCase (admin confirm delivery)

---

Published Events

ShipmentCreatedEvent

ShipmentDeliveredEvent

---

Consumed Events

PaymentCompletedEvent (payment) — auto-creates a shipment for the paid order

---

Allowed Dependencies

order (spi: OrderQuery read — snapshot the delivery address + OrderLifecycle command — advance the
order to PROCESSING on creation and DELIVERED on delivery, after commit via in-module
@ApplicationModuleListeners)

payment (events named interface — consumes PaymentCompletedEvent)

> Note: shipment → order and shipment → payment; neither order nor payment depends back on shipment,
> so the module graph stays acyclic. Order status is *pushed* forward through the order `spi`.

---

# NOTIFICATION MODULE

Package

com.company.ecommerce.notification

---

Responsibilities

* Email notifications
* SMS notifications
* Push notifications

---

Aggregate Roots

None

---

Database Tables

notification_logs

---

Public APIs

None

Event-driven module only

---

Published Events

NotificationSentEvent

---

Consumed Events

UserRegisteredEvent

OrderCreatedEvent

PaymentCompletedEvent

ShipmentCreatedEvent

ShipmentDeliveredEvent

---

Allowed Dependencies

None

---

# REVIEW MODULE

Package

com.company.ecommerce.review

---

Responsibilities

* Product reviews
* Ratings
* Moderation

---

Aggregate Roots

Review

Rating

---

Database Tables

reviews

ratings

---

Public APIs

CreateReviewUseCase

GetReviewsUseCase

DeleteReviewUseCase

---

Published Events

ReviewCreatedEvent

---

Consumed Events

OrderCompletedEvent

---

Allowed Dependencies

catalog

user

---

# REPORTING MODULE

Package

com.company.ecommerce.reporting

---

Responsibilities

* Sales reports
* Customer reports
* Inventory reports

---

Aggregate Roots

None

Read-only projection module

---

Database Tables

report_views

materialized_reports

---

Public APIs

SalesReportUseCase

InventoryReportUseCase

CustomerReportUseCase

---

Published Events

None

---

Consumed Events

OrderCompletedEvent

PaymentCompletedEvent

ShipmentDeliveredEvent

ReviewCreatedEvent

---

Allowed Dependencies

None

---

# AUDIT MODULE

Package

com.company.ecommerce.audit

---

Responsibilities

* Business activity logging
* Security auditing
* Compliance tracking

---

Aggregate Roots

AuditLog

---

Database Tables

audit_logs

---

Public APIs

SearchAuditLogUseCase

---

Published Events

None

---

Consumed Events

ALL BUSINESS EVENTS

---

Allowed Dependencies

None

---

# Shared/Common Module

Package

com.company.ecommerce.common

Purpose:

Shared technical concerns only.

Allowed Content:

* BaseEntity
* AuditableEntity
* Exceptions
* Constants
* Security Utilities
* Common DTOs
* Result Wrapper

Forbidden Content:

* Business Logic
* Domain Services
* Module-specific Entities

---

# Event Ownership Rule

Event names must be owned by the publishing module.

Example:

OrderCreatedEvent

Owner:

order module

Consumers:

payment

notification

audit

(Inventory is *not* an event consumer here — order reserves/releases stock through the inventory
`spi` to keep the module graph acyclic. See the ORDER module note.)

Consumers must never modify the Order aggregate.

---

# Aggregate Ownership Rule

Each aggregate has exactly one owner module.

Example:

Order

Owner:

order module

Only order module may:

* Create
* Update
* Delete

Other modules may only reference OrderId.

Never reference another module's entity directly.

Use IDs and events.

---

# Module Verification Rule

Spring Modulith verification tests must pass.

Example:

ApplicationModules.of(EcommerceApplication.class)
.verify();

A merge request cannot be approved if module verification fails.

---

# Definition of Done For New Module

A module is considered complete only when:

* Responsibilities defined
* Aggregate root defined
* Database tables defined
* Public API defined
* Events defined
* Tests implemented
* Flyway migrations added
* Documentation updated
* Modulith verification passes


---
## Module Completion Workflow

When a module is complete:

1. Run tests
2. Run mvn clean verify
3. Summarize changes
4. Wait for user review

After approval:

5. Commit using Conventional Commits
6. Push feature branch
7. Create PR description

After PR approval:

8. Squash merge PR
9. Delete feature branch
10. Switch to main
11. Pull latest changes
12. Update DEVELOPMENT_STATUS.md
13. Report completion