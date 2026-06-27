# OBSERVABILITY.md

# E-Commerce Platform Observability Guide

Phase 8 — Observability. This document describes the metrics, dashboards, alerts, health checks and
distributed tracing wired into the platform.

> For **operational response** (what to do when an alert fires, how to triage an incident), see the
> companion [`OBSERVABILITY_RUNBOOK.md`](OBSERVABILITY_RUNBOOK.md).

The stack follows the standard Spring Boot observability path:

```
Application → Micrometer → Prometheus → Grafana
                  │
                  └── Micrometer Tracing (Brave) → Zipkin
```

---

## 1. Endpoints

Exposed via Spring Boot Actuator (`management.endpoints.web.exposure.include`):

| Endpoint                          | Purpose                                                        |
| --------------------------------- | ------------------------------------------------------------- |
| `/actuator/health`                | Aggregate health (DB, Redis, metrics contributor)             |
| `/actuator/health/liveness`       | Liveness probe (process is up)                                |
| `/actuator/health/readiness`      | Readiness probe — `readinessState` + `db` + `redis`           |
| `/actuator/info`                  | Build info (version, artifact, build time), Java, OS          |
| `/actuator/metrics`               | Browse individual meters (JSON)                               |
| `/actuator/prometheus`            | Prometheus scrape endpoint (OpenMetrics text)                 |

All actuator endpoints are `permitAll` in `SecurityConfig` so Prometheus can scrape without auth on
the internal Docker network; full health details are shown only to authorized callers
(`management.endpoint.health.show-details: when_authorized`). In production these sit behind the
reverse proxy and are not exposed publicly.

---

## 2. Metrics

### Common tags

Every meter is tagged (via `MetricsConfig`) with:

- `application` — `ecommerce`
- `environment` — the active Spring profile (`local` / `docker` / …)

so all series can be filtered/grouped consistently. HTTP URI tag cardinality is capped at 100 to
protect Prometheus from unbounded paths.

### Technical metrics (auto-instrumented by Micrometer)

| Area      | Example meters                                                          |
| --------- | ----------------------------------------------------------------------- |
| JVM       | `jvm_memory_used_bytes`, `jvm_gc_pause_seconds`, `jvm_threads_live_threads` |
| CPU       | `process_cpu_usage`, `system_cpu_usage`                                 |
| HTTP      | `http_server_requests_seconds_*` (histogram + p50/p95/p99 + SLO buckets)|
| DB pool   | `hikaricp_connections_active|idle|pending`                              |
| Redis     | `lettuce_command_completion_seconds_*`                                  |

HTTP server request latency publishes histogram buckets and percentiles
(`management.metrics.distribution.*`) so Grafana can render heatmaps and accurate quantiles.

### Business metrics (custom)

Incremented from published domain events by `BusinessMetricsEventHandlers` (in the cross-cutting
`config` module, on the same post-commit `@ApplicationModuleListener` channel used by `audit` and
`reporting` — so no business module depends on Micrometer). All are pre-registered at startup so the
series exist before the first event.

| Meter (Micrometer name)        | Prometheus name                              | Source event             |
| ------------------------------ | -------------------------------------------- | ------------------------ |
| `ecommerce.orders.placed`      | `ecommerce_orders_placed_orders_total`       | `OrderCreatedEvent`      |
| `ecommerce.payments.completed` | `ecommerce_payments_completed_payments_total`| `PaymentCompletedEvent`  |
| `ecommerce.payments.failed`    | `ecommerce_payments_failed_payments_total`   | `PaymentFailedEvent`     |
| `ecommerce.shipments.delivered`| `ecommerce_shipments_delivered_shipments_total` | `ShipmentDeliveredEvent` |
| `ecommerce.reviews.created`    | `ecommerce_reviews_created_reviews_total`    | `ReviewCreatedEvent`     |
| `ecommerce.users.registered`   | `ecommerce_users_registered_users_total`     | `UserRegisteredEvent`    |

---

## 3. Prometheus

Config: `monitoring/prometheus.yml`. Scrapes `app:8080/actuator/prometheus` every 15s, attaches
`external_labels` (`monitor`, `environment`), and loads alert rules from `monitoring/alerts.yml`.

UI: http://localhost:9090 (targets at `/targets`, alerts at `/alerts`).

### Alert rules (`monitoring/alerts.yml`)

| Alert                       | Condition                                             | Severity |
| --------------------------- | ----------------------------------------------------- | -------- |
| `ApplicationDown`           | `up == 0` for 1m                                       | critical |
| `HighHttpErrorRate`         | 5xx ratio > 5% for 5m                                  | warning  |
| `HighRequestLatency`        | p95 latency > 1s for 5m                                | warning  |
| `JvmHeapPressure`           | heap used / max > 90% for 5m                           | warning  |
| `DbConnectionPoolExhausted` | `hikaricp_connections_pending > 0` for 2m             | warning  |

Rules are evaluated by Prometheus; wiring an Alertmanager for routing/notification is a later
production-readiness step.

---

## 4. Grafana

Config: `monitoring/grafana/`. Datasource and dashboards are **auto-provisioned** on startup
(`provisioning/datasources/`, `provisioning/dashboards/`), so no manual setup is needed.

UI: http://localhost:3000 (default `admin` / `admin`, override via `GRAFANA_ADMIN_*`).

Bundled dashboards (folder **E-Commerce**):

| Dashboard (uid)                 | Contents                                                          |
| ------------------------------- | ---------------------------------------------------------------- |
| JVM & System (`ecommerce-jvm`)  | Heap/non-heap, threads, GC, CPU                                  |
| HTTP & Resources (`ecommerce-http`) | Request rate, 5xx ratio, p50/p95/p99 latency, top endpoints, HikariCP, Redis |
| Business KPIs (`ecommerce-business`) | Orders, payments (success/fail), users, shipments, reviews  |

Dashboard JSON lives in `monitoring/grafana/dashboards/`.

---

## 5. Distributed tracing

`micrometer-tracing-bridge-brave` + `zipkin-reporter-brave` instrument incoming HTTP requests (and
downstream calls), propagate `traceId`/`spanId`, and export spans to **Zipkin**.

- Zipkin UI: http://localhost:9411
- Sampling: `management.tracing.sampling.probability` (`TRACING_SAMPLE_RATE`, default `1.0` in
  dev — lower in production).
- Export target: `ZIPKIN_ENDPOINT` (defaults to the `zipkin` compose service). If unreachable,
  spans are dropped silently — tracing never blocks or fails a request.

### Log correlation

`traceId`/`spanId` are injected into the logging MDC:

- Plain console (local): appended via `logging.pattern.correlation`.
- Docker profile: structured **ECS JSON** logs (`logging.structured.format.console: ecs`) to stdout,
  with trace fields included — satisfying the deployment logging contract (timestamp, traceId, …).

---

## 6. Quick verification

```bash
docker compose up -d --build

# Health & readiness
curl -s localhost:8080/actuator/health | jq
curl -s localhost:8080/actuator/health/readiness | jq

# Custom business metric is present
curl -s localhost:8080/actuator/prometheus | grep ecommerce_orders_placed

# Prometheus scrape target is UP
open http://localhost:9090/targets

# Grafana dashboards (auto-provisioned, folder "E-Commerce")
open http://localhost:3000

# Traces (after exercising some endpoints)
open http://localhost:9411
```
