# Phase 0 — Foundation (smoke tests)

Phase 0 ships no business APIs. What it *does* give you is the runtime everything else depends on:
the app boots, runs Flyway, connects to Postgres and Redis, and exposes health, metrics and
Swagger. Run these checks first — if any of them fail, the later phases can't work.

**Prerequisites:** the stack running and `export BASE=http://localhost:8080`
(see [README](README.md#start-the-application)). None of these endpoints need a token.

---

## 1. Liveness / readiness

```bash
curl -s $BASE/actuator/health | jq
```

Expected — overall status is `UP` once Postgres **and** Redis are connected and migrations ran:

```json
{ "status": "UP" }
```

> Component details (`db`, `redis`, `diskSpace`, …) are configured as `show-details:
> when_authorized`, so the public response is just the top-level status. Kubernetes-style
> probes are also enabled:

```bash
curl -s $BASE/actuator/health/liveness  | jq   # {"status":"UP"}
curl -s $BASE/actuator/health/readiness | jq   # {"status":"UP"}
```

A `503`/`DOWN` here means a dependency is missing — check `docker compose ps` and that
`docker compose up -d postgres redis` succeeded.

---

## 2. Build info

```bash
curl -s $BASE/actuator/info | jq
```

Returns build/app info (may be `{}` if not configured — that's fine, the endpoint just needs to
respond `200`).

---

## 3. Prometheus metrics

```bash
curl -s $BASE/actuator/prometheus | head -n 20
```

Expected: Prometheus exposition text (lines like `jvm_memory_used_bytes{...}`). This is the
endpoint Prometheus scrapes for the Grafana dashboards.

Only `health`, `info` and `prometheus` are exposed over HTTP (per `application.yml`); other
actuator endpoints intentionally return `404`.

---

## 4. OpenAPI / Swagger UI

| What | URL |
| ---- | --- |
| Swagger UI (browser) | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |

```bash
# Confirm the spec builds and list every documented path
curl -s $BASE/v3/api-docs | jq '.paths | keys'
```

Open the Swagger UI in a browser to explore and try endpoints interactively. Use the **Authorize**
button (top right) to paste a bearer token — get one from the [Phase 1 guide](PHASE1-auth-user.md).

---

## 5. Migrations applied (optional)

If you want to confirm Flyway ran every migration through Phase 3:

```bash
docker exec -i ecommerce-postgres \
  psql -U ecommerce -d ecommerce \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Expected versions: `1` (init) through `7` (coupon) — all `success = t`.

---

## Other stack UIs

When you run the **full** Docker stack (`docker compose up -d --build`):

| Service | URL |
| ------- | --- |
| Prometheus | <http://localhost:9090> |
| Grafana | <http://localhost:3000> (admin / admin) |
| Mailpit (mailbox UI) | <http://localhost:8025> |

---

Once `health` is `UP` and the OpenAPI spec loads, move on to
[Phase 1 — Auth & User](PHASE1-auth-user.md).