# E-Commerce Platform

A production-grade e-commerce platform built as a **Modular Monolith** with **Spring Modulith**.

- **Java** 21 · **Spring Boot** 3.5.x · **Spring Modulith** 1.4.x · **Maven**
- **PostgreSQL** 17 (Flyway migrations) · **Redis** (cache) · **Spring Security** (JWT, from Phase 1)
- **OpenAPI/Swagger** · **Actuator + Prometheus + Grafana** · **Docker / Docker Compose**

See [`docs/`](docs) for the full design (`ARCHITECTURE.md`, `MODULES.md`, `API_GUIDE.md`,
`ROADMAP.md`, `DEPLOYMENT.md`, ADRs). Current status: see [`DEVELOPMENT_STATUS.md`](DEVELOPMENT_STATUS.md).

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
docker compose up -d --build  # app + postgres + redis + mailpit + prometheus + grafana
```

| Service           | URL                                      |
| ----------------- | ---------------------------------------- |
| API health        | http://localhost:8080/actuator/health    |
| Swagger UI        | http://localhost:8080/swagger-ui.html    |
| OpenAPI JSON      | http://localhost:8080/v3/api-docs        |
| Prometheus        | http://localhost:9090                    |
| Grafana           | http://localhost:3000 (admin/admin)      |
| Mailpit (mail UI) | http://localhost:8025                    |

Stop with `docker compose down` (add `-v` to also drop volumes).

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
├── config                      # security, redis, openapi, jpa auditing
└── auth, user, catalog, ...    # business modules (api / application / domain / infrastructure)
```

Modules communicate only through published events or public module APIs — enforced by
`ModularityTests`.