# API Test Guides

Hands-on, **copy-pasteable** guides for running and testing the platform's APIs yourself,
phase by phase. Unlike [`../API_GUIDE.md`](../API_GUIDE.md) (a forward-looking reference that
also lists endpoints from phases not built yet), every request in these guides targets an
**implemented, working** endpoint and has been checked against the controllers and DTOs in the
codebase.

| Guide | Modules | Endpoints |
| ----- | ------- | --------- |
| [PHASE0 — Foundation](PHASE0-foundation.md) | — | Health, actuator, Swagger, Prometheus |
| [PHASE1 — Auth & User](PHASE1-auth-user.md) | `auth`, `user` | register, login, refresh, logout, profile, addresses |
| [PHASE2 — Catalog & Inventory](PHASE2-catalog-inventory.md) | `catalog`, `inventory` | brands, categories, products, search, stock reserve/release |
| [PHASE3 — Cart & Coupon](PHASE3-cart-coupon.md) | `cart`, `coupon` | cart items, coupon create/validate/apply |
| [PHASE4 — Order](PHASE4-order.md) | `order` | place/get/list/cancel orders, admin status lifecycle |
| [PHASE5 — Payment & Shipment](PHASE5-payment-shipment.md) | `payment`, `shipment` | process/get/list payments, admin refund, shipment create/track/status/deliver |
| [PHASE6 — Notification & Review](PHASE6-notification-review.md) | `notification`, `review` | event-driven emails (Mailpit), create/list/delete reviews, rating summary, admin moderation |
| [PHASE7 — Reporting & Audit](PHASE7-reporting-audit.md) | `reporting`, `audit` | admin sales/product reports, audit-log search, per-user activity timeline |

Run the guides **in order** within a phase — later steps reuse IDs and tokens captured by
earlier ones.

---

## Prerequisites

- The stack running locally (see [Start the application](#start-the-application)).
- [`curl`](https://curl.se/) and [`jq`](https://jqlang.github.io/jq/) on your `PATH`
  (`jq` is used to pull values like tokens and IDs out of the JSON responses).
- A POSIX shell (bash/zsh). All snippets `export` shell variables so the next step can reuse them —
  **run each phase's steps in one terminal session.**

---

## Start the application

The default Spring profile points at `localhost`, so you only need Postgres + Redis in Docker:

```bash
# from the repo root
docker compose up -d postgres redis      # infra only
./mvnw spring-boot:run                    # run the app on :8080
```

…or run the whole stack in Docker:

```bash
cp .env.example .env                      # JWT_SECRET is required for the app container
docker compose up -d --build
```

Either way the API is at **`http://localhost:8080`**. Set this once per terminal session:

```bash
export BASE=http://localhost:8080
```

Confirm it's up before testing anything else:

```bash
curl -s $BASE/actuator/health | jq
# {"status":"UP"}
```

---

## The response envelope

**Every** endpoint returns the same success envelope:

```json
{ "success": true, "message": "Success", "data": { } }
```

Errors use a parallel envelope produced by the `GlobalExceptionHandler`:

```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [ { "field": "email", "message": "must not be blank" } ]
}
```

`errors` is only populated for field-level (400) validation failures; business errors
(404/409) carry just a `message`.

| Status | Meaning in this app |
| ------ | ------------------- |
| `200` | OK |
| `201` | Resource created |
| `204` | Deleted (no body) |
| `400` | Bean-validation failure on the request body/params |
| `401` | Missing/invalid/expired access token |
| `403` | Authenticated but lacks `ROLE_ADMIN` |
| `404` | Resource not found |
| `405` | Wrong HTTP method for the route |
| `409` | Business-rule violation (duplicate slug/SKU/code, insufficient stock, coupon not applicable, …) |

---

## Authentication

All endpoints require a JWT **except** `POST /api/v1/auth/register`, `/login`, and `/refresh`.
Send the access token as a bearer header:

```
Authorization: Bearer <accessToken>
```

Access tokens live **15 minutes**; refresh tokens live **7 days**. When an access token expires
you'll get `401` — call `/auth/refresh` (see the Phase 1 guide) or just log in again.

A quick helper to log in and capture a token in one go (used throughout the guides):

```bash
login() {
  curl -s $BASE/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$2\"}" | jq -r '.data.accessToken'
}

export TOKEN=$(login john@example.com 'Password123!')
echo $TOKEN
```

---

## Getting an admin token

Registration **always** creates a `ROLE_CUSTOMER` account — there is no self-service admin signup
and no seeded admin user. The admin-only endpoints (`/api/v1/admin/**` in catalog, inventory and
coupon) therefore need a user that has been promoted directly in the database.

> **The role is baked into the access token at login.** Promote the user **first**, *then* log in
> (or log in again) so the new token carries `ROLE_ADMIN`. A token minted before promotion stays
> `ROLE_CUSTOMER` until it's refreshed or re-issued.

```bash
# 1. Register the would-be admin like any other user
curl -s $BASE/api/v1/auth/register -H 'Content-Type: application/json' -d '{
  "email": "admin@example.com",
  "password": "Password123!",
  "firstName": "Ada",
  "lastName": "Admin"
}' | jq

# 2. Promote it to ADMIN in Postgres (container name: ecommerce-postgres; db/user: ecommerce)
docker exec -i ecommerce-postgres \
  psql -U ecommerce -d ecommerce \
  -c "UPDATE auth_users SET role = 'ADMIN' WHERE email = 'admin@example.com';"

# 3. Log in AFTER promotion to get a token with ROLE_ADMIN
export ADMIN_TOKEN=$(login admin@example.com 'Password123!')
echo $ADMIN_TOKEN
```

> Not using Docker for Postgres? Connect however you normally would and run the same
> `UPDATE auth_users SET role = 'ADMIN' WHERE email = '…';` statement.

---

## Pagination & sorting

Collection endpoints (currently product list/search) accept Spring Data params:

```
?page=0&size=20&sort=price,desc
```

Pages are **zero-indexed**, default `size` is **20**, max is **100**. Multiple `sort` params are
allowed (`&sort=name,asc&sort=price,desc`). The payload is the standard page envelope:

```json
{
  "content": [ ],
  "page": 0, "size": 20,
  "totalElements": 42, "totalPages": 3,
  "first": true, "last": false
}
```

---

## Tips

- **Pretty-print:** pipe any response through `| jq`.
- **See status codes:** add `-i` (include headers) or `-w '\n%{http_code}\n'` to a `curl` call.
- **Async side effects:** creating a user spawns a customer profile, and creating a product seeds a
  zero-stock inventory record — both happen on a committed-event listener a moment later. If a
  follow-up read 404s immediately, wait a second and retry.
- **Reset everything:** `docker compose down -v` drops the Postgres/Redis volumes (dev data only).