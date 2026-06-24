# ADR-002 PostgreSQL as Primary Database

Status: Accepted

## Context

The platform requires a reliable relational database supporting ACID transactions, indexing, constraints, and future scalability.

## Decision

Use PostgreSQL as the primary database.

## Alternatives Considered

* MySQL
* MariaDB
* MongoDB

## Consequences

### Positive

* Excellent ACID compliance
* Mature ecosystem
* Advanced indexing
* JSONB support
* Strong Spring Boot support

### Negative

* More operational complexity than embedded databases
* Requires database administration knowledge

## Related Documents

* ARCHITECTURE.md
* DEPLOYMENT.md
