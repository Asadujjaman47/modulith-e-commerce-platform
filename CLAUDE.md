# CLAUDE.md

# AI Development Rules

Project: E-Commerce Platform

Architecture: Spring Modulith

Java Version: 21

Spring Boot Version: 3.x

Build Tool: Maven

Database: PostgreSQL

Cache: Redis

---

# Purpose

This document defines mandatory coding standards for all AI agents and human developers.

Applies to:

* Claude Code
* Codex
* Cursor
* OpenCode
* RooCode
* Windsurf
* Human Developers

If this document conflicts with generated code, this document wins.

---

# Core Principles

1. Business modules first.

2. Package by feature.

3. Events over direct coupling.

4. DTOs at boundaries.

5. Constructor injection only.

6. Test business logic.

7. Flyway for schema changes.

8. Keep modules independent.

9. Prefer readability over cleverness.

10. Production-ready code only.

---

# Architecture Rules

Application architecture is Spring Modulith.

Never create:

controller
service
repository
entity

packages at root level.

Use:

com.company.ecommerce
```
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
```
All business functionality must belong to exactly one module.

---

# Module Structure

Each module must follow:

module
```
├── api
├── application
├── domain
└── infrastructure
```

---

api

Controllers

Request DTOs

Response DTOs

---

application

Use Cases

Application Services

---

domain

Entities

Value Objects

Domain Events

Business Rules

---

infrastructure

Repositories

Persistence

External Integrations

---

# Dependency Rules

Allowed

Controller

↓

Use Case

↓

Domain

↓

Repository

Forbidden

Controller

↓

Repository

Forbidden

Module A

↓

Internal Class Of Module B

Modules may only communicate through:

* Published events
* Public module APIs

Never access another module's internal package.

---

# Spring Rules

Use:

@RequiredArgsConstructor

Example
```
@Service
@RequiredArgsConstructor
public class OrderService {
}
```
Never use:

@Autowired field injection

Example
```
@Autowired
private OrderService service;
```
Forbidden.

---

# DTO Rules

Controllers must never expose:

* Entities
* Aggregates
* JPA models

Controllers return DTOs only.

Example

Good

ProductResponse

Bad

Product

---

Request DTO naming

CreateProductRequest

UpdateProductRequest

---

Response DTO naming

ProductResponse

OrderResponse

CustomerResponse

---

# Java Rules

Use Java 21 features where appropriate.

Allowed

record

sealed interfaces

switch expressions

Optional

Text Blocks

Forbidden

Legacy Date API

Example

Date

Calendar

Use:

Instant

LocalDate

LocalDateTime

OffsetDateTime

---

# Entity Rules

Use JPA entities only inside module boundaries.

Entities must not be returned from controllers.

Every aggregate root must extend:

AuditableEntity

Example fields

createdAt

updatedAt

createdBy

updatedBy

---

# Repository Rules

Repositories belong in:

infrastructure

Example

catalog.infrastructure.persistence

Never inject repositories into controllers.

---

# MapStruct Rules

Use MapStruct for all entity-to-DTO mapping.

Do not manually map DTOs unless necessary.

Required

@Mapper(componentModel = "spring")

Example

ProductMapper

OrderMapper

CustomerMapper

---

# Validation Rules

All external requests must be validated.

Use:

@NotNull

@NotBlank

@Email

@Positive

@Size

Validation must happen at DTO level.

---

# API Rules

Base URL

/api/v1

Use plural resource names.

Good

/products

/orders

/users

Bad

/getProducts

/createOrder

/updateUser

---

# Response Standard

Success
```
{
"success": true,
"message": "Success",
"data": {}
}
```
Error
```
{
"success": false,
"message": "Validation failed",
"errors": []
}
```
All controllers must follow this format.

---

# Exception Handling Rules

Use:

@RestControllerAdvice

Create:

GlobalExceptionHandler

Handle:

ValidationException

BusinessException

EntityNotFoundException

AccessDeniedException

Never use try/catch in controllers.

---

# Event Rules

Prefer events over direct module calls.

Example

OrderCreatedEvent

↓

Inventory Module

↓

Payment Module

↓

Notification Module

Create events in:

domain.event

Example

order.domain.event.OrderCreatedEvent

Events should be immutable.

Prefer Java records.

Example

public record OrderCreatedEvent(
UUID orderId
) {}

---

# Transaction Rules

Use:

@Transactional

at application service level.

Do not create long-running transactions.

Never span multiple module operations in a single transaction.

---

# Database Rules

All schema changes require Flyway migration.

Create:

V1__initial_schema.sql

V2__catalog.sql

V3__inventory.sql

Never modify old migration files.

Create new migrations only.

Forbidden

Updating production schema manually.

---

# PostgreSQL Rules

Use UUID primary keys.

Example

@Id
private UUID id;

Avoid database-generated numeric IDs.

Use indexes for:

Foreign Keys

Search Columns

Unique Constraints

---

# Redis Rules

Use Redis for:

Cart

Product Cache

Coupon Cache

Sessions

Do not store business-critical data exclusively in Redis.

Redis is cache.

PostgreSQL remains source of truth.

---

# Security Rules

Authentication

JWT

Authorization

ROLE_ADMIN

ROLE_CUSTOMER

Passwords

BCrypt

Never store:

Plain text passwords

Secrets

Tokens in logs

---

# Logging Rules

Use:

@Slf4j

Example

log.info(
"Order created. id={}",
orderId
);

Never use:

System.out.println()

Never log:

Passwords

JWT Tokens

Secrets

Payment Credentials

---

# Testing Rules

Every use case requires tests.

Required

Unit Tests

Integration Tests

Module Tests

Use:

JUnit 5

Mockito

Testcontainers

Naming

PlaceOrderUseCaseTest

ProductControllerTest

InventoryIntegrationTest

---

# Coverage Rules

Minimum

80%

Critical flows

Authentication

Checkout

Order Creation

Payment

Inventory Reservation

Require integration testing.

---

# OpenAPI Rules

Every endpoint must contain:

@Operation

@Schema

Response examples

Swagger must build successfully.

---

# Docker Rules

Application must run using:

docker compose up

without manual intervention.

Application startup must:

Run Flyway

Connect PostgreSQL

Connect Redis

Expose health endpoint

---

# Health Checks

Required

/actuator/health

/actuator/prometheus

Application is considered healthy only when:

Database connected

Redis connected

Migrations successful

---

# Documentation Rules

Every major feature requires updates to:

MODULES.md

API_GUIDE.md

ARCHITECTURE.md

when applicable.

Documentation is part of the feature.

---

# Code Generation Rules For AI

When generating code:

1. Generate complete implementations.

2. Include validation.

3. Include tests.

4. Include logging.

5. Include DTOs.

6. Include MapStruct mapper.

7. Include Flyway migration if schema changes.

8. Include OpenAPI annotations.

9. Follow module boundaries.

10. Follow Spring Modulith principles.

Never generate placeholder code unless explicitly requested.

Never generate TODO comments as implementation.

Always generate production-ready code.

---

# Definition Of Done

Code is complete only when:

* Compiles
* Tests pass
* Validation exists
* Logging exists
* DTOs exist
* OpenAPI annotations exist
* Flyway migration exists
* Module boundaries respected
* Documentation updated
* Docker startup succeeds
