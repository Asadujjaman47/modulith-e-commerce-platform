# ADR-005 Flyway Database Migrations

Status: Accepted

## Context

Database schema changes must be versioned, repeatable, and automated.

## Decision

Use Flyway for schema migrations.

## Alternatives Considered

* Manual SQL execution
* Liquibase

## Consequences

### Positive

* Version-controlled schema
* Repeatable deployments
* CI/CD friendly

### Negative

* Migration discipline required
* Old migrations cannot be modified

## Related Documents

* DEPLOYMENT.md
* CLAUDE.md
