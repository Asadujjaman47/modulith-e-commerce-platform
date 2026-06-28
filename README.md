# E-Commerce Platform

[![CI](https://github.com/Asadujjaman47/modulith-e-commerce-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Asadujjaman47/modulith-e-commerce-platform/actions/workflows/ci.yml)
[![Release](https://github.com/Asadujjaman47/modulith-e-commerce-platform/actions/workflows/release.yml/badge.svg)](https://github.com/Asadujjaman47/modulith-e-commerce-platform/actions/workflows/release.yml)

A production-grade e-commerce platform built as a **Modular Monolith** with **Spring Modulith**.

- **Java** 21 · **Spring Boot** 3.5.x · **Spring Modulith** 1.4.x · **Maven**
- **PostgreSQL** 17 (Flyway migrations) · **Redis** (cache) · **Spring Security** (JWT, from Phase 1)
- **OpenAPI/Swagger** · **Actuator + Micrometer + Prometheus + Grafana + Zipkin (tracing)** · **Docker / Docker Compose**

See [`docs/`](docs) for the full design (`ARCHITECTURE.md`, `MODULES.md`, `API_GUIDE.md`,
`ROADMAP.md`, `DEPLOYMENT.md`, ADRs). Current status: see [`DEVELOPMENT_STATUS.md`](DEVELOPMENT_STATUS.md).
For hands-on, copy-pasteable curl walkthroughs to run and test each phase's APIs yourself, see
[`docs/api-testing/`](docs/api-testing).

**Implemented so far:** all phases (0–9) plus CI/CD are complete. Phase 0 (foundation), Phase 1
(`auth` + `user`) — registration, login, JWT refresh with rotation, logout, customer profile and
address management — Phase 2 (`catalog` + `inventory`) — products, categories, brands with
paginated/sortable search, plus stock tracking with reservation and release — Phase 3
(`cart` + `coupon`) — a per-customer cart with live stock checks and price snapshots, plus coupon
create/validate/apply — Phase 4 (`order`) — place an order from the cart (optional coupon,
idempotent), cancel it, order history/details, and an admin status lifecycle, with stock
reservation/release, cart clearing and coupon-usage recording driven as post-commit side effects —
Phase 5 (`payment` + `shipment`) — process a payment through a pluggable gateway (status, history,
admin refund) and create a shipment with tracking that auto-advances the order PAID → PROCESSING →
DELIVERED — Phase 6 (`notification` + `review`) — event-driven email notifications (Mailpit in dev)
and product reviews/ratings behind a purchase gate — Phase 7 (`reporting` + `audit`) — sales/product
report projections and an append-only audit trail of every business event — Phase 8 (observability)
— Micrometer → Prometheus → Grafana metrics with business KPIs, plus Micrometer Tracing → Zipkin and
trace-correlated JSON logs — Phase 9 (production readiness) — hardened exception handling, security
headers / CORS / actuator lockdown + a `prod` profile, Redis caching, Bucket4j rate limiting, and
JDBC/pool performance tuning. **CI/CD** adds GitHub Actions (build/test, CodeQL, Trivy) publishing
images to GHCR plus a gated SSH `docker compose` deploy (see [`docs/CICD.md`](docs/CICD.md)).
Endpoints under `/api/v1/auth/*`, `/api/v1/users/*`, `/api/v1/products|categories|brands*` (incl.
`/products/{id}/reviews`), `/api/v1/cart*`, `/api/v1/coupons/*`, `/api/v1/orders*`,
`/api/v1/payments*`, `/api/v1/shipments/*`, `/api/v1/reviews/*`, and `/api/v1/admin/*` (catalog,
inventory, coupons, orders, payments, shipments, reviews, reports, audit-logs) — browse them in
Swagger UI.

## Prerequisites

- JDK 21
- Docker + Docker Compose
- No local Maven needed — use the bundled wrapper (`./mvnw`).

## Run locally (IntelliJ or terminal)

The default profile points at `localhost`, so you only need Postgres + Redis running:

```bash
# 1. Start infrastructure
docker compose up -d postgres redis

# 2a. Run from the terminal (live reload of the running process)
./mvnw spring-boot:run

# 2b. …or run the built jar
./mvnw clean package
java -jar target/ecommerce-0.1.0-SNAPSHOT.jar
```

In **IntelliJ**: open the project, then run the `EcommerceApplication` main class — the default
profile works as-is. Configuration can be overridden with the `DB_*` / `REDIS_*` env vars (see
[`.env.example`](.env.example)).

## Run the full stack (everything in Docker)

```bash
cp .env.example .env          # adjust values if needed
docker compose up -d --build  # app + postgres + redis + mailpit + prometheus + grafana + zipkin
```

| Service           | URL                                      |
| ----------------- | ---------------------------------------- |
| API health        | http://localhost:8080/actuator/health    |
| Swagger UI        | http://localhost:8080/swagger-ui.html    |
| OpenAPI JSON      | http://localhost:8080/v3/api-docs        |
| Prometheus        | http://localhost:9090                    |
| Grafana           | http://localhost:3000 (admin/admin)      |
| Zipkin (tracing)  | http://localhost:9411                    |
| Mailpit (mail UI) | http://localhost:8025                    |

Grafana ships with auto-provisioned dashboards (JVM/System, HTTP & Resources, Business KPIs) and a
Prometheus datasource. See [`docs/OBSERVABILITY.md`](docs/OBSERVABILITY.md) (reference) and
[`docs/OBSERVABILITY_RUNBOOK.md`](docs/OBSERVABILITY_RUNBOOK.md) (alert response / incident triage).

Stop with `docker compose down` (add `-v` to also drop volumes).

> **Postgres major-version mismatch.** The DB lives in the `postgres-data` named volume, which is
> stamped with the Postgres major version that first created it. If you upgrade the `postgres` image
> major version in `docker-compose.yml` (e.g. 16 → 17), an existing volume will block startup with:
>
> ```
> FATAL: database files are incompatible with server
> DETAIL: The data directory was initialized by PostgreSQL version 16, ... not compatible with this version 17.
> ```
>
> The image is pinned to **Postgres 17** to match the Testcontainers integration tests. To reset the
> local dev database (safe — it holds only throwaway dev data):
>
> ```bash
> docker compose down -v                                   # drop all stack volumes, or…
> docker volume rm modulith-e-commerce-platform_postgres-data   # just the DB volume
> ```

## Test

```bash
./mvnw clean verify
```

Runs unit tests, the Spring Modulith boundary verification, and the Testcontainers integration test
(requires a running Docker daemon).

## Project layout

```
com.company.ecommerce
├── EcommerceApplication        # @Modulithic entry point
├── common                      # shared: AuditableEntity, ApiResponse/ErrorResponse, exceptions
├── config                      # redis, openapi, jpa auditing, observability (metrics/tracing/health)
└── auth, user, catalog, ...    # business modules (api / application / domain / infrastructure)
```

Modules communicate only through published events or public module APIs — enforced by
`ModularityTests`.