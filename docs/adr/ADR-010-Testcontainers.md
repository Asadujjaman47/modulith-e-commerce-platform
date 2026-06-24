# ADR-010 Testcontainers for Integration Testing

Status: Accepted

## Context

Integration tests should run against infrastructure that closely matches production.

## Decision

Use Testcontainers with PostgreSQL and Redis.

## Alternatives Considered

* H2 Database
* Embedded databases
* Mock-only integration testing

## Consequences

### Positive

* Production-like behavior
* Higher confidence in tests
* Fewer environment-specific bugs

### Negative

* Slower test execution
* Docker dependency during testing

## Related Documents

* CLAUDE.md
* DEPLOYMENT.md
