# SECURITY.md

Security posture and hardening reference for the E-Commerce Platform.

---

## Authentication & Authorization

- Stateless JWT access tokens (HMAC-SHA256, 15 min) + opaque refresh tokens (7 days, SHA-256 hashed
  in Postgres). See `auth` module.
- `SecurityConfig` (owned by `auth`) is stateless (`SessionCreationPolicy.STATELESS`), CSRF disabled
  (no server-side sessions/cookies), method security (`@PreAuthorize`) enabled.
- Roles: `ROLE_CUSTOMER`, `ROLE_ADMIN`. Admin endpoints are guarded with `@PreAuthorize("hasRole('ADMIN')")`.
- Passwords hashed with BCrypt. Secrets/tokens are never logged.

### JWT signing secret

- Configured via `app.jwt.secret` (base64, ≥ 256 bits). Overridable with `JWT_SECRET`.
- Local/dev (`application.yml`) carries a **public placeholder** secret for convenience.
- The **`prod` profile removes the fallback**: `JWT_SECRET` must be supplied or the application
  fails to start (the HMAC key requires ≥ 256 bits).

---

## Public vs. protected endpoints

Public (no token):

- `POST /api/v1/auth/register`, `/login`, `/refresh`
- `GET /actuator/health`, `/actuator/health/**`, `/actuator/info`
- Swagger UI / OpenAPI (`/swagger-ui/**`, `/v3/api-docs/**`) — **disabled in the `prod` profile**

Admin-only:

- All `/api/v1/admin/**` business endpoints
- **`/actuator/**` except health/info** — `prometheus`, `metrics`, `modulith`, etc. expose internal
  detail and require `ROLE_ADMIN`.

Everything else requires a valid access token.

---

## HTTP security headers

Applied to every response by `SecurityConfig`:

| Header | Value |
| ------ | ----- |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Strict-Transport-Security` | `max-age=31536000 ; includeSubDomains` (emitted over HTTPS only) |

## CORS

- Bound from `app.cors.*` (`CorsProperties`), applied to `/api/**`.
- `allowed-origins` defaults to `http://localhost:3000` for dev; **the `prod` profile has no default**
  and requires `CORS_ALLOWED_ORIGINS`. Exact origins only (no wildcard) because credentialed
  requests are allowed.

---

## Rate limiting

See [docs/RATE_LIMITING.md](RATE_LIMITING.md). Auth endpoints are limited per client IP; authenticated
API traffic per principal. Exceeded limits return HTTP 429 with the standard error envelope and a
`Retry-After` header.

---

## Scraping protected metrics

Because `/actuator/prometheus` is now `ROLE_ADMIN`-only, Prometheus must present an admin bearer
token. Provide a token to the Prometheus container and enable the `authorization` block in
`monitoring/prometheus.yml`:

```yaml
authorization:
  type: Bearer
  credentials_file: /etc/prometheus/scrape-token
```

Mount a file containing a valid admin access token at that path. (For environments that need a
long-lived scrape credential, prefer a dedicated admin service account whose token is rotated by
your secrets manager.) Until a token is configured, the bundled Grafana app-metrics panels will be
empty while health/info probes continue to work unauthenticated.

---

## Production checklist

- [ ] `SPRING_PROFILES_ACTIVE` includes `prod` (e.g. `docker,prod`)
- [ ] `JWT_SECRET` set to a strong, rotated secret (≥ 32 bytes, base64)
- [ ] `CORS_ALLOWED_ORIGINS` set to the real front-end origin(s)
- [ ] Database/Redis credentials supplied via environment/secrets manager
- [ ] TLS terminated in front of the app (so HSTS is emitted)
- [ ] Prometheus scrape token configured (see above)
