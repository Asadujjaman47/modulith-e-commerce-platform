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

---

Published Events

StockReservedEvent

StockReleasedEvent

StockUpdatedEvent

---

Consumed Events

ProductCreatedEvent (catalog) — seeds a zero-stock inventory record

OrderCreatedEvent (deferred until the order module exists)

OrderCancelledEvent (deferred until the order module exists)

---

Allowed Dependencies

catalog

---

# CART MODULE

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

AddToCartUseCase

UpdateCartItemUseCase

RemoveCartItemUseCase

CheckoutCartUseCase

---

Published Events

CartCheckedOutEvent

---

Consumed Events

ProductUpdatedEvent

StockUpdatedEvent

---

Allowed Dependencies

catalog

inventory

---

# COUPON MODULE

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

ValidateCouponUseCase

ApplyCouponUseCase

CreateCouponUseCase

---

Published Events

CouponAppliedEvent

CouponExpiredEvent

---

Consumed Events

OrderCreatedEvent

---

Allowed Dependencies

None

---

# ORDER MODULE

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

OrderItem

OrderAddress

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

---

Published Events

OrderCreatedEvent

OrderCancelledEvent

OrderCompletedEvent

---

Consumed Events

CartCheckedOutEvent

CouponAppliedEvent

---

Allowed Dependencies

cart

coupon

user

---

# PAYMENT MODULE

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

CreatePaymentUseCase

VerifyPaymentUseCase

RefundPaymentUseCase

---

Published Events

PaymentCompletedEvent

PaymentFailedEvent

PaymentRefundedEvent

---

Consumed Events

OrderCreatedEvent

---

Allowed Dependencies

order

---

# SHIPMENT MODULE

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

TrackingRecord

---

Database Tables

shipments

tracking_records

---

Public APIs

CreateShipmentUseCase

TrackShipmentUseCase

MarkDeliveredUseCase

---

Published Events

ShipmentCreatedEvent

ShipmentDeliveredEvent

---

Consumed Events

PaymentCompletedEvent

---

Allowed Dependencies

order

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

inventory

payment

notification

audit

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