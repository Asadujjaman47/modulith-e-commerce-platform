# ADR-006 Redis Caching Strategy

Status: Accepted

## Context

Product catalog, cart operations, and coupon validation can generate significant database load.

## Decision

Use Redis as the distributed cache layer.

## Alternatives Considered

* In-memory cache only
* Hazelcast

## Consequences

### Positive

* Faster reads
* Reduced database load
* Shared cache across instances

### Negative

* Additional infrastructure
* Cache invalidation complexity

## Related Documents

* ARCHITECTURE.md
* DEPLOYMENT.md
