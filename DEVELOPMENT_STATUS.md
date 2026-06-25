# Current Milestone

Phase 3 — Cart (next)

## Completed

### Phase 0 — Foundation (merged to `main`, PR #1)

- Spring Boot 3.5.15 + Spring Modulith 1.4.12 project skeleton (Maven, Java 21)
- Package-by-module structure with 15 module boundaries
- `common` module: AuditableEntity, ApiResponse/ErrorResponse envelope, exceptions, GlobalExceptionHandler
- `config` module: JPA auditing, Redis, OpenAPI, baseline Security (stateless, BCrypt)
- PostgreSQL + Flyway (V1__init.sql baseline)
- Redis integration
- OpenAPI / Swagger UI
- Actuator (health, info, prometheus)
- Dockerfile + docker-compose (postgres, redis, mailpit, app, prometheus, grafana)
- Tests: Modulith verification, Testcontainers context load, unit tests

### Phase 1 — Authentication & User Management

- `auth` module
  - Aggregates: `UserCredential` (auth_users), `RefreshToken` (refresh_tokens)
  - Use cases: register, login, refresh (with rotation), logout (refresh-token revocation)
  - JWT access tokens (15 min) + opaque refresh tokens (7 days, SHA-256 hashed in Postgres)
  - `JwtAuthenticationFilter` + JWT-protected `SecurityConfig` (owned by auth); `@EnableMethodSecurity`
  - Custom 401/403 JSON responses via the standard error envelope
  - Events published: `UserRegisteredEvent`, `UserLoggedInEvent` (exposed as a named interface)
- `user` module
  - Aggregates: `Customer` (customers), `Address` (customer_addresses)
  - Profile management (`GET/PUT /api/v1/users/me`) and address CRUD (`/api/v1/users/me/addresses`)
  - MapStruct mappers (`CustomerMapper`, `AddressMapper`)
  - Consumes `UserRegisteredEvent` via `@ApplicationModuleListener` to create the customer profile
  - Events published: `CustomerCreatedEvent`, `CustomerUpdatedEvent`, `AddressAddedEvent`
- Flyway: `V2__auth.sql`, `V3__user.sql`
- JWT dependency (jjwt 0.12.6) + `app.jwt.*` configuration (env-overridable `JWT_SECRET`)
- Tests: 42 passing — use-case unit tests (Mockito), JWT provider tests, full Testcontainers
  integration test (register → async customer creation → login → profile/address → refresh → logout),
  Modulith verification

### Phase 2 — Catalog & Inventory

- `catalog` module
  - Aggregates: `Category` (categories), `Brand` (brands), `Product` (products) with `ProductImage`
    children (product_images)
  - Use cases: create/update/get/delete/search products; manage categories & brands
  - Search via JPA `Specification`s: keyword (name/description/sku), category, brand and price-range
    filters, with pagination + multi-sort (`PageResponse` envelope, default size 20, max 100)
  - Public (authenticated) browsing: `GET /api/v1/products`, `/products/{id}`, `/products/search`,
    `/categories`, `/categories/{id}`, `/brands`, `/brands/{id}`
  - Admin (`ROLE_ADMIN`, `@PreAuthorize`): `/api/v1/admin/products|categories|brands` CRUD
  - MapStruct mappers (`ProductMapper`, `CategoryMapper`, `BrandMapper`)
  - Events published (named interface): `ProductCreatedEvent`, `ProductUpdatedEvent`,
    `ProductDeletedEvent`
- `inventory` module
  - Aggregates: `Inventory` (inventory), `StockReservation` (stock_reservations),
    `InventoryTransaction` (inventory_transactions, append-only ledger)
  - Use cases: get/update stock, reserve stock, release stock (availability invariant enforced in
    the domain; over-reservation → HTTP 409)
  - Admin API (`ROLE_ADMIN`): `GET/PUT /api/v1/admin/inventory/{productId}`,
    `POST /api/v1/admin/inventory/reserve`, `POST /api/v1/admin/inventory/release`
  - Consumes catalog `ProductCreatedEvent` (`@ApplicationModuleListener`) to seed a zero-stock
    inventory record — the only `inventory -> catalog` coupling
  - Events published (named interface): `StockReservedEvent`, `StockReleasedEvent`,
    `StockUpdatedEvent`
  - Note: `OrderCreatedEvent` / `OrderCancelledEvent` consumers are deferred until the `order`
    module exists; reservation/release are driven through the admin API for now
- `common`: shared `PageResponse<T>` pagination envelope; `spring.data.web.pageable` defaults
- Flyway: `V4__catalog.sql`, `V5__inventory.sql`
- Tests: 67 passing — use-case unit tests (Mockito), `Inventory` domain invariants, and a full
  Testcontainers integration test (admin creates brand/category/product → async inventory seeding →
  browse/search/paginate → set stock → reserve → release; over-reserve 409; non-admin 403),
  Modulith verification

## In Progress

None

## Next

- Phase 3: Cart (`cart` module) — shopping cart and cart-item management

## Current Branch

feature/catalog-inventory