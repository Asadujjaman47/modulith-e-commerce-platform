# Phase 1 — Authentication & User Management

Modules: **`auth`** (register / login / refresh / logout, JWT) and **`user`** (customer profile +
addresses). Registering a user publishes `UserRegisteredEvent`, which the `user` module consumes to
create the customer profile asynchronously.

**Prerequisites:** stack running, `export BASE=http://localhost:8080`
(see [README](README.md)). No token needed to start — you'll mint one here.

Run the steps top-to-bottom in one terminal session.

---

## 1. Register

`POST /api/v1/auth/register` — public. Always creates a `ROLE_CUSTOMER` account.

```bash
curl -s -i $BASE/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "john@example.com",
    "password": "Password123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**`201 Created`:**

```json
{
  "success": true,
  "message": "Registration successful",
  "data": { "userId": "…uuid…", "email": "john@example.com" }
}
```

Password rules (enforced by validation): 8–72 chars with **at least one** uppercase, lowercase,
digit and special character. A weak password → `400`; an already-registered email → `409`.

```bash
# 400 — weak password (note the field-level error)
curl -s $BASE/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"weak@example.com","password":"weak","firstName":"A","lastName":"B"}' | jq
```

---

## 2. Login

`POST /api/v1/auth/login` — public. Returns an access/refresh token pair.

```bash
curl -s $BASE/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{ "email": "john@example.com", "password": "Password123!" }' | jq
```

**`200 OK`:**

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOi…",
    "refreshToken": "…opaque…",
    "expiresIn": 900,
    "tokenType": "Bearer"
  }
}
```

Capture both tokens for the next steps:

```bash
LOGIN=$(curl -s $BASE/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{ "email": "john@example.com", "password": "Password123!" }')
export TOKEN=$(echo "$LOGIN"   | jq -r '.data.accessToken')
export REFRESH=$(echo "$LOGIN" | jq -r '.data.refreshToken')
echo "access:  $TOKEN"
echo "refresh: $REFRESH"
```

Wrong credentials → `401` (`{"success":false,"message":"Invalid credentials", …}`).

---

## 3. Get profile

`GET /api/v1/users/me` — authenticated. The profile was created asynchronously when you registered.

```bash
curl -s $BASE/api/v1/users/me -H "Authorization: Bearer $TOKEN" | jq
```

**`200 OK`:**

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": "…uuid…",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "phone": null,
    "createdAt": "2026-06-25T10:00:00Z"
  }
}
```

> Just registered and seeing `404`? The profile is created on a committed-event listener a beat
> after registration. Wait a second and retry.

Try it **without** a token to see the `401` path:

```bash
curl -s -o /dev/null -w '%{http_code}\n' $BASE/api/v1/users/me   # 401
```

---

## 4. Update profile

`PUT /api/v1/users/me` — authenticated. `firstName`/`lastName` are required; `phone` is optional
and must match E.164-ish `^\+?[0-9]{7,15}$`.

```bash
curl -s $BASE/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{ "firstName": "Johnny", "lastName": "Doe", "phone": "+14155552671" }' | jq
```

`200 OK` returns the updated `CustomerResponse`. An invalid phone → `400`.

---

## 5. Addresses (CRUD)

Base path `GET/POST /api/v1/users/me/addresses`, item path `PUT/DELETE …/{addressId}`.
All authenticated.

### Create

```bash
ADDR=$(curl -s $BASE/api/v1/users/me/addresses \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "label": "Home",
    "line1": "221B Baker Street",
    "line2": "Apt 4",
    "city": "London",
    "state": "Greater London",
    "postalCode": "NW1 6XE",
    "country": "GB",
    "defaultAddress": true
  }')
echo "$ADDR" | jq
export ADDRESS_ID=$(echo "$ADDR" | jq -r '.data.id')
```

**`201 Created`** with the created `AddressResponse`. Required fields: `line1`, `city`,
`postalCode`, `country` — omitting any → `400`.

### List

```bash
curl -s $BASE/api/v1/users/me/addresses -H "Authorization: Bearer $TOKEN" | jq
```

`data` is an array of addresses.

### Update

```bash
curl -s $BASE/api/v1/users/me/addresses/$ADDRESS_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "label": "Home",
    "line1": "221B Baker Street",
    "line2": "Flat 4",
    "city": "London",
    "state": "Greater London",
    "postalCode": "NW1 6XE",
    "country": "GB",
    "defaultAddress": true
  }' | jq
```

`200 OK`. An unknown `addressId` → `404`.

### Delete

```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  -X DELETE $BASE/api/v1/users/me/addresses/$ADDRESS_ID \
  -H "Authorization: Bearer $TOKEN"
# 204
```

---

## 6. Refresh tokens (rotation)

`POST /api/v1/auth/refresh` — public (the refresh token itself is the credential). The presented
refresh token is **revoked** and a brand-new pair is issued (rotation).

```bash
REFRESHED=$(curl -s $BASE/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d "{ \"refreshToken\": \"$REFRESH\" }")
echo "$REFRESHED" | jq
# rotate our captured values to the new pair
export TOKEN=$(echo "$REFRESHED"   | jq -r '.data.accessToken')
export REFRESH=$(echo "$REFRESHED" | jq -r '.data.refreshToken')
```

`200 OK` with a fresh `accessToken`/`refreshToken`. Re-using the **old** refresh token now → `401`
(it was revoked on rotation). Try it to confirm:

```bash
# the pre-rotation refresh token is no longer valid → 401
curl -s -o /dev/null -w '%{http_code}\n' $BASE/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d "{ \"refreshToken\": \"$REFRESH\" }" >/dev/null   # (this one is the NEW token; still valid)
```

---

## 7. Logout

`POST /api/v1/auth/logout` — **authenticated** (send the access token) and pass the refresh token
to revoke. The short-lived access token then simply expires on its own.

```bash
curl -s $BASE/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{ \"refreshToken\": \"$REFRESH\" }" | jq
```

**`200 OK`:** `{"success":true,"message":"Logout successful","data":null}`.
After this the revoked refresh token can no longer be exchanged at `/refresh`.

---

## Endpoint summary

| Method | Path | Auth | Notes |
| ------ | ---- | ---- | ----- |
| POST | `/api/v1/auth/register` | public | always `ROLE_CUSTOMER`; 409 on duplicate email |
| POST | `/api/v1/auth/login` | public | returns token pair |
| POST | `/api/v1/auth/refresh` | public | rotates: old refresh token revoked |
| POST | `/api/v1/auth/logout` | bearer | revokes the supplied refresh token |
| GET | `/api/v1/users/me` | bearer | profile (created async on register) |
| PUT | `/api/v1/users/me` | bearer | update names/phone |
| GET | `/api/v1/users/me/addresses` | bearer | list |
| POST | `/api/v1/users/me/addresses` | bearer | 201 |
| PUT | `/api/v1/users/me/addresses/{addressId}` | bearer | 404 if unknown |
| DELETE | `/api/v1/users/me/addresses/{addressId}` | bearer | 204 |

Next: [Phase 2 — Catalog & Inventory](PHASE2-catalog-inventory.md) (needs an **admin** token —
see [Getting an admin token](README.md#getting-an-admin-token)).