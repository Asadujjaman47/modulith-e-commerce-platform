# Current Milestone

Phase 2 — Catalog (next)

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

## In Progress

None

## Next

- Phase 2: Catalog (`catalog` module) — products, categories, brands, images, search

## Current Branch

main