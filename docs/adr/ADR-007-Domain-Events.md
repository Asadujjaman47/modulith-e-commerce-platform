# ADR-007 Domain Events for Module Communication

Status: Accepted

## Context

Modules should remain loosely coupled while still reacting to business actions.

## Decision

Use Spring Modulith domain events as the primary communication mechanism between modules.

## Alternatives Considered

* Direct service calls
* Shared repositories

## Consequences

### Positive

* Lower coupling
* Better modularity
* Easier future extraction to microservices

### Negative

* Event flow becomes harder to trace
* Event ordering considerations

## Related Documents

* MODULES.md
* ARCHITECTURE.md
