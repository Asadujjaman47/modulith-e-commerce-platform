# ADR-006 Redis Caching Strategy

Status: Accepted

## Context

Product catalog, cart operations, and coupon validation can generate significant database load.

## Decision

Use Redis as the distributed cache layer.

Wired in Phase 9 via the Spring Cache abstraction (`config.CacheConfig`, `@EnableCaching` +
`RedisCacheManager`): catalog product-by-id and the category/brand lists are cached with per-cache
TTLs and event/write-driven eviction. Cache failures degrade to a database read
(`LoggingCacheErrorHandler`); see ARCHITECTURE.md §17. Coupon-by-code is not cached (its read has
lazy-expiry side effects) and cart remains in PostgreSQL.

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
