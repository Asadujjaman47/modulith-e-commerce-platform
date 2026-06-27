# RATE_LIMITING.md

API rate limiting for the E-Commerce Platform.

---

## Overview

Rate limiting is implemented with [Bucket4j](https://bucket4j.com/) token buckets. Buckets are stored
in **Redis** (via Lettuce) so limits are enforced **across all application instances**. If Redis is
unreachable at startup, the limiter degrades to **per-instance in-memory buckets** — limiting still
works, just not cluster-wide.

The limiter is a servlet filter (`RateLimitFilter`) scoped to `/api/*`, registered just after the
Spring Security filter chain so the authenticated principal is available for keying. It **fails open**:
if the backing store errors mid-request, the request is allowed rather than blocked.

---

## Policies

| Scope | Endpoints | Key | Default limit |
| ----- | --------- | --- | ------------- |
| `auth` | `/api/v1/auth/**` (login, register, refresh) | client IP | 10 / minute |
| `api` | all other `/api/**` | principal (or IP if anonymous) | 100 / minute |

The auth policy is deliberately strict to blunt credential stuffing / brute force. The client IP is
taken from `X-Forwarded-For` (first hop) when present, else the remote address — so deploy behind a
trusted proxy that sets this header.

---

## Responses

- On success: `X-RateLimit-Remaining` header reports the tokens left in the bucket.
- On exhaustion: **HTTP 429** with the standard error envelope and a `Retry-After` (seconds) header:

```json
{ "success": false, "message": "Too many requests. Please retry later." }
```

---

## Configuration (`app.rate-limit.*`)

| Property | Env | Default |
| -------- | --- | ------- |
| `enabled` | `RATE_LIMIT_ENABLED` | `true` |
| `auth.capacity` | `RATE_LIMIT_AUTH_CAPACITY` | `10` |
| `auth.refill-period` | `RATE_LIMIT_AUTH_PERIOD` | `PT1M` |
| `api.capacity` | `RATE_LIMIT_API_CAPACITY` | `100` |
| `api.refill-period` | `RATE_LIMIT_API_PERIOD` | `PT1M` |

Set `RATE_LIMIT_ENABLED=false` to turn the filter off entirely (it is disabled in the test suite so
the shared loopback IP never trips it).
