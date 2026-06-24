# ADR-003 UUID Primary Keys

Status: Accepted

## Context

The application exposes resource identifiers through public APIs and may evolve toward distributed systems.

## Decision

Use UUID as primary keys for all aggregates.

## Alternatives Considered

* Long
* Integer
* Database Sequences

## Consequences

### Positive

* Globally unique
* Safe for distributed systems
* Prevents ID enumeration attacks

### Negative

* Larger indexes
* Slightly higher storage requirements

## Related Documents

* MODULES.md
* API_GUIDE.md
