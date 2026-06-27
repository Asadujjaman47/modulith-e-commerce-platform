# DEPLOYMENT.md

# E-Commerce Platform Deployment Guide

Version: 1.0

Application Type: Spring Modulith

Runtime: Java 21

Deployment Strategy: Docker

Infrastructure: Docker Compose

---

# 1. Purpose

This document defines:

* Local deployment
* Development deployment
* Production deployment
* Docker standards
* Environment variables
* Monitoring setup
* Backup strategy
* CI/CD deployment process

---

# 2. Deployment Architecture

Production Stack

Internet

↓

Reverse Proxy

(Nginx)

↓

Spring Boot Application

↓

PostgreSQL

↓

Redis

↓

Prometheus

↓

Grafana

↓

Zipkin (Tracing)

↓

Mailpit (Non-Production)

---

# 3. Deployment Environments

## Local

Purpose:

Developer machine

Profile:

local

Database:

Docker PostgreSQL

Cache:

Docker Redis

Monitoring:

Optional

---

## Development

Purpose:

Shared development environment

Profile:

dev

Database:

Dedicated PostgreSQL

Cache:

Dedicated Redis

Monitoring:

Enabled

---

## Staging

Purpose:

Pre-production validation

Profile:

staging

Database:

Dedicated PostgreSQL

Cache:

Dedicated Redis

Monitoring:

Enabled

Production-like configuration

---

## Production

Purpose:

Customer-facing environment

Profile:

prod

Database:

Production PostgreSQL

Cache:

Production Redis

Monitoring:

Mandatory

---

# 4. Docker Services

Required Containers

* ecommerce-app
* postgres
* redis
* prometheus
* grafana

Optional

* mailpit
* pgadmin

---

# 5. Docker Network

Network Name

ecommerce-network

All containers must communicate through the internal Docker network.

Never expose internal services publicly.

Allowed Public Ports

80

443

3000 (optional)

8080 (development only)

---

# 6. Docker Volumes

Persistent Volumes (current dev compose)

postgres-data

grafana-data

Redis runs as a cache only (no persistence) in development — PostgreSQL remains the source of
truth. `redis-data` and `prometheus-data` are added in Phase 9 (Production Readiness) when Redis
persistence and long-term metrics retention are enabled.

Backups must include all persistent volumes.

---

# 7. Dockerfile Standards

Requirements

* Multi-stage build
* Java 21
* Non-root user
* Health check enabled
* Small runtime image

Example Flow

Build Stage

↓

Package JAR

↓

Runtime Stage

↓

Run Application

Never use:

latest

Always pin versions.

---

# 8. Container Security Rules

Required

* Run as non-root user
* Read-only filesystem where possible
* Environment variables for secrets
* No credentials in source code

Forbidden

* Hardcoded passwords
* Root containers
* Exposed database ports in production

Running containers as non-root users and using multi-stage builds are recommended production practices.

---

# 9. Spring Profiles

Current Profiles (implemented)

default — local development. `application.yml`. Datasource/Redis default to localhost so the app
can run from IntelliJ or the terminal against `docker compose up -d postgres redis`.

docker — application running inside a container. `application-docker.yml`. Datasource/Redis target
the compose service hostnames (`postgres`, `redis`).

Planned Profiles (Phase 9 — Production Readiness)

dev

staging

prod

To be added as `application-dev.yml`, `application-staging.yml`, `application-prod.yml`.

Startup Example

SPRING_PROFILES_ACTIVE=docker

---

# 10. Environment Variables

Required

DB_HOST

DB_PORT

DB_NAME

DB_USERNAME

DB_PASSWORD

REDIS_HOST

REDIS_PORT

JWT_SECRET (base64 HMAC-SHA256 secret, >= 32 bytes; required on docker/deploy — no fallback)

JWT_ACCESS_TTL (ISO-8601 duration, default PT15M)

JWT_REFRESH_TTL (ISO-8601 duration, default P7D)

MAIL_HOST

MAIL_PORT

MAIL_USERNAME

MAIL_PASSWORD

APP_BASE_URL

---

Two-layer naming

The PostgreSQL container image requires `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`. The
application reads its datasource from the `DB_*` variables above. In `docker compose` the app's
`DB_*` values are derived from the single `POSTGRES_*` source of truth, so credentials are defined
once. See `.env.example` for the full template.

JWT_* variables are implemented (Phase 1, auth). `JWT_SECRET` has a dev-only fallback in
`application.yml` for local runs, but the Docker/deploy path supplies no fallback and fails fast if
it is unset. MAIL_* variables arrive in Phase 6 (notifications).

---

# 11. Secret Management

Local

.env

Development

Docker Secrets

Production

Secret Manager

Never commit:

.env

*.pem

*.key

credentials.json

---

# 12. Database Deployment

Database

PostgreSQL

Version

17+

Schema Management

Flyway

Application startup must fail if migrations fail.

No manual schema updates allowed.

---

# 13. Redis Deployment

Purpose

* Cart Cache
* Product Cache
* Session Cache
* Coupon Cache

Persistence

Enabled

Max Memory Policy

allkeys-lru

---

# 14. Reverse Proxy

Technology

Nginx

Responsibilities

* SSL termination
* Compression
* Security headers
* Rate limiting
* Request logging

Public URL

https://api.company.com

Internal URL

http://ecommerce-app:8080

---

# 15. HTTPS Requirements

Production requires HTTPS.

Certificates

Let's Encrypt

or

Cloudflare Origin Certificates

HTTP traffic must redirect to HTTPS.

---

# 16. Application Health Checks

Spring Boot Actuator

Endpoints

/actuator/health

/actuator/info

/actuator/prometheus

Container health checks should use the Actuator health endpoint.

---

# 17. Monitoring Stack

Application

↓

Micrometer

↓

Prometheus

↓

Grafana

Expose

/actuator/prometheus

Monitor

* JVM
* CPU
* Memory
* HTTP Requests
* Database Connections
* Redis Connections
* Cache Hit Rate

Actuator + Micrometer + Prometheus + Grafana is a widely used Spring Boot observability stack.

Implemented in Phase 8. The Grafana datasource and dashboards (JVM/System, HTTP & Resources,
Business KPIs) are auto-provisioned from `monitoring/grafana/provisioning`, and Prometheus loads
alert rules from `monitoring/alerts.yml`. Custom business counters (orders, payments, shipments,
reviews, registrations) are emitted alongside the technical metrics. Distributed tracing
(Micrometer Tracing → Brave → Zipkin) and trace-correlated JSON logs are wired in the same phase.
See `docs/OBSERVABILITY.md`.

---

# 18. Logging Strategy

Format

JSON

Required Fields

timestamp

traceId

userId

module

action

status

Log Levels

ERROR

WARN

INFO

DEBUG (non-production only)

Never log

Passwords

JWT Tokens

Credit Card Data

Secrets

Implemented in Phase 8: the `docker` profile emits structured **ECS JSON** logs to stdout
(`logging.structured.format.console`), with `traceId`/`spanId` injected into the MDC by Micrometer
Tracing for trace ↔ log correlation. Local runs keep readable console output with a correlation
suffix.

---

# 19. Backup Strategy

PostgreSQL

Daily

Retention

30 Days

Redis

Daily Snapshot

Retention

7 Days

Grafana

Weekly Export

Retention

30 Days

---

# 20. Recovery Strategy

Database Recovery

Restore latest backup

Apply latest migrations

Verify integrity

Application Recovery

Redeploy latest stable image

Verify health endpoint

Verify database connectivity

---

# 21. Resource Limits

Development

CPU

1 Core

Memory

1 GB

---

Production

CPU

2+ Cores

Memory

2+ GB

Container JVM settings should respect container memory limits.

---

# 22. CI/CD Pipeline

GitHub Actions

Pipeline Stages

1. Build

2. Unit Tests

3. Integration Tests

4. Static Analysis

5. Docker Build

6. Docker Push

7. Deploy

8. Smoke Tests

Deployment fails if any stage fails.

---

# 23. Deployment Process

Step 1

Pull latest image

Step 2

Run Flyway migrations

Step 3

Start application

Step 4

Verify health endpoint

Step 5

Verify database connectivity

Step 6

Verify Prometheus metrics

Step 7

Verify Grafana dashboard

Step 8

Enable traffic

---

# 24. Production Readiness Checklist

Application

[ ] Health checks enabled

[ ] Flyway configured

[ ] Redis configured

[ ] Logging configured

[ ] Monitoring configured

[ ] Security configured

[ ] HTTPS configured

[ ] Docker image scanned

[ ] Backups configured

[ ] Alerts configured

Infrastructure

[ ] PostgreSQL healthy

[ ] Redis healthy

[ ] Prometheus healthy

[ ] Grafana healthy

[ ] Reverse proxy healthy

---

# 25. Definition of Done

Deployment is complete only when:

* Docker image builds successfully
* Application starts successfully
* Flyway migrations succeed
* Health endpoint returns UP
* Database connectivity verified
* Redis connectivity verified
* Metrics visible in Prometheus
* Dashboards visible in Grafana
* CI/CD pipeline passes
* Rollback procedure documented
