# ADR-018 API Rate Limiting

Status: Accepted

## Context

Public endpoints (especially authentication) are exposed to credential-stuffing and brute-force
attempts, and any client can overload the API with request bursts. The platform runs as a modular
monolith that may be scaled to multiple instances, so limits must hold across instances.

## Decision

Use [Bucket4j](https://bucket4j.com/) token buckets, stored in **Redis** via the Lettuce
`ProxyManager`, enforced by a servlet filter (`RateLimitFilter`) on `/api/*`. Two policies: a strict
per-IP limit on `/api/v1/auth/**` and a looser per-principal (or per-IP) limit on the rest of the API.
Exceeding a limit returns HTTP 429 with the standard error envelope and a `Retry-After` header.

## Alternatives Considered

* Spring Cloud Gateway / API gateway rate limiting — no gateway in the current topology.
* Resilience4j RateLimiter — in-process, not cluster-wide without extra work.
* Nginx/ingress-level limiting — coarse, no per-principal keying or app-level envelope.

## Consequences

### Positive

* Limits shared across instances (Redis-backed).
* Per-principal and per-IP keying; tunable via `app.rate-limit.*`.
* Fails open and degrades to in-memory buckets if Redis is unavailable — availability over strictness.

### Negative

* Adds Bucket4j + a dedicated Lettuce client.
* Requests behind a proxy rely on a trusted `X-Forwarded-For`.

## Related Documents

* RATE_LIMITING.md
* SECURITY.md
* ARCHITECTURE.md
