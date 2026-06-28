# ROADMAP.md

# E-Commerce Platform Development Roadmap

Project: E-Commerce Platform

Architecture: Spring Modulith

Java: 21

Spring Boot: 3.x

Database: PostgreSQL

Deployment: Docker

Status: Phase 0–8 complete (Foundation, Auth & User, Catalog & Inventory, Cart & Coupon, Order, Payment & Shipment, Notification & Review, Reporting & Audit, Observability) — Phase 9 (Production Readiness) next

---

# Purpose

This roadmap defines:

* Development phases
* Milestones
* Sprint goals
* Dependencies
* Deliverables
* Definition of completion

This roadmap is the implementation order that all AI agents and developers should follow.

---

# Project Timeline

Phase 0

Foundation

↓

Phase 1

Authentication & User Management

↓

Phase 2

Catalog & Inventory

↓

Phase 3

Cart & Coupon

↓

Phase 4

Order Management

↓

Phase 5

Payment & Shipment

↓

Phase 6

Notifications & Reviews

↓

Phase 7

Reporting & Audit

↓

Phase 8

Observability

↓

Phase 9

Production Readiness

↓

Phase 10

CI/CD & Deployment

---

# PHASE 0

# Foundation

Status: Complete (merged to main, PR #1)

Goal

Create project skeleton and establish architecture.

Duration

1 Week

---

Deliverables

* Spring Boot project
* Spring Modulith setup
* PostgreSQL integration
* Flyway integration
* Redis integration
* Docker setup
* OpenAPI setup
* Global exception handling
* Base response model
* Base auditing model

---

Modules

common

config

---

Tasks

Create package structure

Create module boundaries

Configure Flyway

Configure Redis

Configure Swagger

Configure Actuator

Create Dockerfile

Create docker-compose.yml

---

Success Criteria

Application starts successfully

Database migration works

Docker build succeeds

Swagger available

Health endpoint available

---

# PHASE 1

# Authentication & User Management

Status: Complete — register / login / refresh (with rotation) / logout, JWT (15 min access, 7 day
refresh), ROLE_ADMIN/ROLE_CUSTOMER, customer profile + address management. 42 tests passing;
verified from terminal and Docker.

Goal

Enable user registration and login.

Duration

1 Week

---

Modules

auth

user

---

Features

User Registration

Login

Refresh Token

Logout

Profile Management

Address Management

---

Deliverables

JWT Authentication

Role Management

Customer Profile

Customer Address

---

Events

UserRegisteredEvent

UserLoggedInEvent

CustomerCreatedEvent

CustomerUpdatedEvent

AddressAddedEvent

---

Success Criteria

User can register ✓

User can login ✓

JWT generated ✓

Profile retrieved successfully ✓

---

# PHASE 2

# Catalog & Inventory

Status: Complete — catalog (products / categories / brands with paginated, sortable, filterable
search) and inventory (stock tracking, reservation, release; inventory seeded from the catalog
`ProductCreatedEvent`). Admin CRUD under `/api/v1/admin/*`; public browsing under
`/api/v1/products|categories|brands`. Flyway `V4__catalog.sql`, `V5__inventory.sql`.

Goal

Enable product management and stock management.

Duration

2 Weeks

---

Modules

catalog

inventory

---

Features

Category Management

Brand Management

Product Management

Stock Management

Stock Reservation

Stock Release

---

Deliverables

Product Search

Pagination

Filtering

Sorting

Inventory Tracking

---

Events

ProductCreatedEvent

ProductUpdatedEvent

StockReservedEvent

StockReleasedEvent

---

Success Criteria

Admin can manage products

Customers can search products

Stock tracking works

Inventory reservations work

---

# PHASE 3

# Cart & Coupon

Status: Complete — per-customer cart (add/update/remove with live inventory checks and product
name/price snapshots) and coupons (admin create; validate/apply against a supplied order amount,
percentage or fixed with validity-window, minimum-order and usage-limit rules). Flyway
`V6__cart.sql`, `V7__coupon.sql`. Released as `v0.4.0`.

Goal

Enable shopping cart and discount handling.

Duration

1 Week

---

Modules

cart

coupon

---

Features

Add To Cart

Update Cart

Remove Item

Apply Coupon

Validate Coupon

---

Deliverables

Cart Management

Coupon Validation

Discount Calculation

Redis Cart Cache

---

Events

CartCheckedOutEvent

CouponAppliedEvent

CouponExpiredEvent

---

Success Criteria

Customer can manage cart

Coupon application works

Discount calculation works

---

# PHASE 4

# Order Management

Status: Complete — place order from the cart (optional coupon, `Idempotency-Key`), cancel, order
history/details, and an admin status lifecycle (guarded transitions). Stock reservation/release,
cart clearing and coupon-usage recording run as post-commit side effects; the order module
orchestrates cart/coupon/inventory via their `spi` to keep the module graph acyclic. Flyway
`V8__order.sql`. 148 tests passing; verified from terminal and Docker. (Pending PR/merge and the
`v0.5.0` release tag.)

Goal

Implement order lifecycle.

Duration

2 Weeks

---

Modules

order

---

Features

Place Order

Cancel Order

View Orders

Order History

Order Status Tracking

---

Deliverables

Order Aggregate

Order Validation

Order Lifecycle

---

Events

OrderCreatedEvent

OrderCancelledEvent

OrderCompletedEvent

---

Success Criteria

Order successfully created

Order cancellation works

Order history available

---

# PHASE 5

# Payment & Shipment

Goal

Complete checkout process.

Duration

2 Weeks

---

Modules

payment

shipment

---

Features

Payment Processing

Payment Verification

Refunds

Shipment Creation

Shipment Tracking

Delivery Confirmation

---

Deliverables

Mock Payment Gateway

Shipment Tracking

Refund Support

---

Events

PaymentCompletedEvent

PaymentFailedEvent

ShipmentCreatedEvent

ShipmentDeliveredEvent

---

Success Criteria

Payment succeeds

Shipment generated

Tracking works

Delivery status updated

---

# PHASE 6

# Notifications & Reviews

Goal

Improve customer engagement.

Duration

1 Week

---

Modules

notification

review

---

Features

Email Notification

SMS Notification

Product Reviews

Ratings

Review Moderation

---

Deliverables

Mailpit Integration

Notification Logs

Review APIs

---

Events

NotificationSentEvent

ReviewCreatedEvent

---

Success Criteria

Notifications sent

Reviews submitted

Ratings visible

---

# PHASE 7

# Reporting & Audit

Goal

Provide operational visibility.

Duration

1 Week

---

Modules

reporting

audit

---

Features

Sales Reports

Customer Reports

Inventory Reports

Audit Logs

Activity Tracking

---

Deliverables

Reporting APIs

Audit Search APIs

Business Event Tracking

---

Success Criteria

Reports generated

Audit trail available

---

# PHASE 8

# Observability

Goal

Monitor application health.

Duration

1 Week

---

Features

Metrics

Monitoring

Dashboards

Alerts

---

Deliverables

Prometheus

Grafana

Micrometer

Actuator

---

Metrics

JVM

CPU

Memory

HTTP Requests

Database Connections

Redis Connections

---

Success Criteria

Metrics visible in Grafana

Prometheus scraping works

Health endpoint available

---

# PHASE 9

# Production Readiness

Goal

Prepare application for production.

Duration

1 Week

---

Features

Performance Optimization

Caching

Security Hardening

Backup Strategy

Recovery Strategy

---

Deliverables

Rate Limiting

Security Headers

Cache Optimization

Backup Procedures

---

Success Criteria

Security review passed

Performance review passed

Backup strategy documented

---

# PHASE 10

# CI/CD & Deployment

Status: ✅ Delivered (PRs #13, #14). GitHub Actions pipeline in `.github/workflows/`
(`build`/`ci`/`release`/`deploy`); images published to GHCR. See `docs/CICD.md`.

Goal

Automate build and deployment.

Duration

1 Week

---

Features

GitHub Actions

Docker Registry

Deployment Automation

Smoke Tests

---

Deliverables

Build Pipeline

Test Pipeline

Docker Push

Deployment Workflow

---

Success Criteria

Pipeline fully automated

Deployment reproducible

Smoke tests pass

---

# MVP Definition

The project is considered MVP complete when:

* Authentication works
* Product catalog works
* Inventory works
* Cart works
* Order creation works
* Payment works
* Shipment works
* Docker deployment works

Target Phase

Phase 5

---

# Production Ready Definition

The project is considered production-ready when:

* Monitoring implemented
* Audit logging implemented
* Reporting implemented
* Security hardened
* CI/CD automated
* Documentation complete

Target Phase

Phase 10

---

# Release Plan

Release 0.1

Foundation ✓

Release 0.2

Authentication & Users ✓

Release 0.3

Catalog & Inventory ✓

Release 0.4

Cart & Coupon ✓

Release 0.5

Orders

Release 0.6

Payments & Shipment

Release 0.7

Notifications & Reviews

Release 0.8

Reporting & Audit

Release 0.9

Observability

Release 1.0

Production Ready

---

# Final Success Criteria

Project completion requires:

* All modules implemented
* Modulith verification passes
* Test coverage >= 80%
* Docker deployment working
* Monitoring working
* Documentation complete
* CI/CD automated
* Release 1.0 deployed
