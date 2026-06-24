# DECISIONS.md

# Architecture Decision Records (ADR) Index

Project: E-Commerce Platform

Architecture: Spring Modulith

Last Updated: 2026-06-24

---

# Purpose

This document is the central index of all Architecture Decision Records (ADRs).

ADRs capture significant architectural and technical decisions made during the project lifecycle, including:

* Architecture style
* Database choices
* Security strategy
* Deployment strategy
* Testing strategy
* Integration patterns
* Observability decisions

Detailed decision records are stored in:

docs/adr/

---

# ADR Status Definitions

| Status     | Meaning                    |
| ---------- | -------------------------- |
| Proposed   | Under discussion           |
| Accepted   | Approved and implemented   |
| Superseded | Replaced by another ADR    |
| Deprecated | No longer recommended      |
| Rejected   | Evaluated but not selected |

---

# Current ADRs

| ADR     | Title                                  | Status   |
| ------- | -------------------------------------- | -------- |
| ADR-001 | Modular Monolith Architecture          | Accepted |
| ADR-002 | PostgreSQL as Primary Database         | Accepted |
| ADR-003 | UUID Primary Keys                      | Accepted |
| ADR-004 | JWT Authentication                     | Accepted |
| ADR-005 | Flyway Database Migrations             | Accepted |
| ADR-006 | Redis Caching Strategy                 | Accepted |
| ADR-007 | Domain Events for Module Communication | Accepted |
| ADR-008 | Docker Deployment Standard             | Accepted |
| ADR-009 | MapStruct DTO Mapping                  | Accepted |
| ADR-010 | Testcontainers for Integration Testing | Accepted |

---

# Future ADR Candidates

Potential future decisions that may require ADRs:

| ADR     | Topic                     |
| ------- | ------------------------- |
| ADR-011 | Elasticsearch Search      |
| ADR-012 | Outbox Pattern            |
| ADR-013 | OpenTelemetry             |
| ADR-014 | Keycloak Integration      |
| ADR-015 | Object Storage (S3/MinIO) |
| ADR-016 | Kafka Event Streaming     |
| ADR-017 | Multi-Tenant Architecture |
| ADR-018 | API Rate Limiting         |
| ADR-019 | Feature Flags             |
| ADR-020 | Distributed Tracing       |

---

# ADR Creation Rules

Create a new ADR when changing:

* Architecture style
* Database technology
* Authentication strategy
* Authorization strategy
* Deployment strategy
* Caching strategy
* Eventing strategy
* Search strategy
* Monitoring strategy
* Testing strategy
* External platform integrations

Do not create ADRs for minor implementation details.

---

# ADR Naming Convention

Format:

ADR-XXX-Short-Title.md

Examples:

ADR-001-Modular-Monolith.md

ADR-002-PostgreSQL.md

ADR-003-UUID-Identifiers.md

ADR-004-JWT-Authentication.md

---

# ADR Location

docs/

└── adr/

├── ADR-001-Modular-Monolith.md

├── ADR-002-PostgreSQL.md

├── ADR-003-UUID-Identifiers.md

├── ADR-004-JWT-Authentication.md

├── ADR-005-Flyway-Migrations.md

├── ADR-006-Redis-Caching.md

├── ADR-007-Domain-Events.md

├── ADR-008-Docker-Deployment.md

├── ADR-009-MapStruct.md

└── ADR-010-Testcontainers.md

---

# Review Process

1. Create ADR.
2. Document alternatives.
3. Document trade-offs.
4. Review.
5. Approve.
6. Mark as Accepted.
7. Implement.
8. Update related documentation.

---

# Related Documents

* PROJECT_VISION.md
* ARCHITECTURE.md
* MODULES.md
* API_GUIDE.md
* DEPLOYMENT.md
* CLAUDE.md
* ROADMAP.md

All architectural decisions must remain consistent with these documents.
