# ADR-009 MapStruct DTO Mapping

Status: Accepted

## Context

The application contains many DTO-to-entity and entity-to-DTO conversions.

## Decision

Use MapStruct for all mapping operations.

## Alternatives Considered

* Manual mapping
* ModelMapper

## Consequences

### Positive

* Compile-time safety
* Better performance
* Reduced boilerplate

### Negative

* Additional generated code
* Build-time dependency

## Related Documents

* CLAUDE.md
* API_GUIDE.md
