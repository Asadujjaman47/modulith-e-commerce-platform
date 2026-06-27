# OBSERVABILITY_RUNBOOK.md

# E-Commerce Platform — Observability Runbook

Operational guide for responding to alerts and investigating issues using the observability stack.
For the reference description of metrics/dashboards/config, see [`OBSERVABILITY.md`](OBSERVABILITY.md).

> **Audience:** on-call engineers and operators. **Goal:** detect → triage → diagnose → mitigate,
> fast, with the tools already wired into the platform.

---

## 1. Tooling quick reference

| Tool        | URL (local/docker)                       | Use for                                        |
| ----------- | ---------------------------------------- | ---------------------------------------------- |
| App health  | http://localhost:8080/actuator/health    | Is the app up? Which dependency is down?       |
| Metrics     | http://localhost:8080/actuator/prometheus| Raw current metric values                      |
| Prometheus  | http://localhost:9090                     | Query metrics, check `/targets` and `/alerts`  |
| Grafana     | http://localhost:3000                     | Dashboards (JVM, HTTP & Resources, Business)   |
| Zipkin      | http://localhost:9411                     | Find slow/failing requests by trace            |
| App logs    | `docker compose logs -f app`             | ECS JSON logs; correlate by `trace.id`         |

First moves for **any** incident:

1. `GET /actuator/health` — confirm the app and its dependencies (db, redis).
2. Grafana → **HTTP & Resources** — request rate, 5xx ratio, p95/p99 latency.
3. Prometheus → http://localhost:9090/alerts — see which alerts are firing/pending.

---

## 2. Alert response playbooks

Alerts are defined in `monitoring/alerts.yml`. Each section below maps an alert to its likely
causes, how to confirm, and how to mitigate.

### 2.1 `ApplicationDown` (critical)

**Means:** Prometheus could not scrape the app for >1m (`up == 0`).

**Confirm**
- Prometheus → `/targets`: is the `ecommerce-app` target `DOWN`? Note `lastError`.
- `docker compose ps` — is `ecommerce-app` running/healthy?
- `curl -fsS localhost:8080/actuator/health/liveness`.

**Likely causes & actions**
- Container crashed / OOM → `docker compose logs --tail=200 app`; look for `OutOfMemoryError` or a
  startup stack trace. Restart: `docker compose up -d app`.
- Flyway migration failed on boot → logs show the failing migration; the app fails fast by design.
  Fix the migration/DB and redeploy. **Never** edit an applied migration — add a new one.
- DB/Redis unreachable at startup → the app waits for them (`depends_on: healthy`); check
  `ecommerce-postgres` / `ecommerce-redis` health.
- Network/port issue → confirm `app:8080` reachable from the Prometheus container.

### 2.2 `HighHttpErrorRate` (warning)

**Means:** >5% of HTTP responses are 5xx over 5m.

**Confirm**
- Grafana → **HTTP & Resources** → *Error Rate (5xx %)* and *Top Endpoints by Request Rate*.
- Prometheus:
  ```promql
  topk(5, sum by (uri,status) (rate(http_server_requests_seconds_count{status=~"5.."}[5m])))
  ```

**Diagnose**
- Identify the offending `uri`/`status`, then open **Zipkin** filtered by that path and sort by
  duration / error to find a failing trace. Grab its `traceId`.
- Search logs for the trace: `docker compose logs app | grep <traceId>` (ECS JSON includes
  `trace.id`). The log line(s) for that trace usually carry the exception.

**Common causes**
- Downstream dependency failing (DB/Redis) → check §2.1 dependency health and HikariCP panel.
- A bad deploy → correlate the spike start with the last release; roll back the image if needed.
- Unhandled edge case in a specific endpoint → fix forward.

### 2.3 `HighRequestLatency` (warning)

**Means:** p95 HTTP latency >1s for 5m.

**Confirm**
- Grafana → **HTTP & Resources** → *Latency p50/p95/p99* and the per-endpoint panel.

**Diagnose**
- **Zipkin** → sort recent traces by duration; the longest spans show where time goes (DB query,
  Redis, external mail, etc.).
- DB pressure → *HikariCP Connections* panel; if `pending > 0`, see §2.5.
- Slow queries → check DB; look for missing indexes (FKs/search columns per the DB rules).
- GC pauses → JVM dashboard *GC Pause Rate* (see §2.4).

**Mitigate**
- Scale the bottleneck (DB pool size, instance memory), add an index, or cache hot reads in Redis.

### 2.4 `JvmHeapPressure` (warning)

**Means:** heap used / max >90% for 5m.

**Confirm**
- Grafana → **JVM & System** → *JVM Heap Used* (vs max), *GC Pause Rate*, *Live Threads*.

**Diagnose & mitigate**
- Steady climb + frequent GC + no recovery → likely a leak or undersized heap. Capture a heap dump
  for analysis, then restart to restore service.
- Sustained legitimate load → raise the container memory limit (the JVM respects cgroup limits).
- Correlate with a deploy; roll back if the regression is new.

### 2.5 `DbConnectionPoolExhausted` (warning)

**Means:** threads have been waiting for a DB connection (`hikaricp_connections_pending > 0`) for >2m.

**Confirm**
- Grafana → **HTTP & Resources** → *HikariCP Connections* (active vs idle vs pending).

**Diagnose**
- Long-running queries/transactions holding connections → check for slow queries and overly broad
  `@Transactional` scopes (the codebase rule: no long-running or multi-module transactions).
- Connection leak (connections not returned) → look for steadily rising `active` that never drops.
- Under-sized pool for current load.

**Mitigate**
- Kill/optimize the offending query; fix the transaction boundary; raise the Hikari max pool size
  if the DB can take it.

---

## 3. Distributed tracing workflow (Zipkin)

1. Open http://localhost:9411.
2. Filter by `serviceName=ecommerce`, a specific `spanName`/path, `minDuration`, or `tagQuery`
   (e.g. `error`).
3. Open a trace → inspect the span timeline to see which operation dominates or errors.
4. Copy the `traceId` and pivot to logs: `docker compose logs app | grep <traceId>`.

**Sampling:** dev samples 100% (`TRACING_SAMPLE_RATE=1.0`). In production this is lowered, so not
every request is traced — reproduce with load if needed, or temporarily raise the sample rate.

**Zipkin down?** Tracing is best-effort: spans are dropped (logged at WARN, then FINE) and requests
are **never** blocked. App behavior is unaffected; only trace visibility is lost.

---

## 4. Logs

- Format: ECS JSON to stdout on the `docker` profile (`logging.structured.format.console=ecs`);
  readable console with a `[app,traceId,spanId]` correlation suffix locally.
- Correlate by trace: every log line carries `trace.id` / `span.id` when a request is in flight.
- Tail: `docker compose logs -f app`. Filter: `docker compose logs app | grep <traceId|orderId|…>`.
- **Never** appears in logs (by policy): passwords, JWTs, card data, secrets.

---

## 5. Business-metric monitoring

The **Business KPIs** dashboard tracks domain health, not just technical health. Watch for:

| Symptom on dashboard                                    | Investigate                                            |
| ------------------------------------------------------- | ------------------------------------------------------ |
| `payments failed` rising / high vs `completed`          | Payment gateway issues; check payment logs & traces    |
| `orders placed` drops to ~0 during normal hours         | Checkout broken upstream — correlate with 5xx/latency  |
| `orders placed` up but `shipments delivered` flat       | Fulfillment/shipment pipeline stalled                  |
| `users registered` flatlines                            | Registration endpoint or auth dependency issue         |

These counters are event-driven (post-commit listeners). A counter that stops moving can mean the
business flow stopped **or** event processing stalled — cross-check the relevant module logs.

---

## 6. Routine verification (post-deploy smoke)

```bash
# 1. App and dependencies healthy
curl -fsS localhost:8080/actuator/health | jq '.status'        # UP
curl -fsS localhost:8080/actuator/health/readiness | jq '.status'

# 2. Build/version is what you expect
curl -fsS localhost:8080/actuator/info | jq '.build'

# 3. Prometheus is scraping the app
curl -fsS 'localhost:9090/api/v1/targets' | jq '.data.activeTargets[]|{job:.labels.job,health}'

# 4. Alert rules loaded, none unexpectedly firing
curl -fsS 'localhost:9090/api/v1/rules' | jq '.data.groups[].rules[].name'
curl -fsS 'localhost:9090/api/v1/alerts' | jq '.data.alerts'

# 5. Grafana provisioned (datasource + dashboards) — creds from GRAFANA_ADMIN_* (.env)
curl -fsS -u "$GRAFANA_ADMIN_USER:$GRAFANA_ADMIN_PASSWORD" 'localhost:3000/api/search?type=dash-db' | jq '.[].title'

# 6. Traces flowing
curl -fsS 'localhost:9411/api/v2/services'
```

---

## 7. Escalation & follow-up

- **Mitigate first** (restart/roll back/scale) to restore service, then root-cause.
- After any incident, capture: the firing alert, the Grafana panel screenshots, the offending
  `traceId`, and the relevant log excerpt.
- If an alert was noisy or missed a real issue, tune the threshold in `monitoring/alerts.yml`.
- Wiring an **Alertmanager** (routing/paging) and per-environment thresholds are part of Phase 9
  (Production Readiness); until then, alerts are visible in the Prometheus UI (`/alerts`).
