# Phase 2 — Catalog & Inventory

Modules: **`catalog`** (brands, categories, products, search) and **`inventory`** (stock levels,
reservations). Creating a product publishes `ProductCreatedEvent`, which `inventory` consumes to
seed a **zero-stock** record for that product automatically.

This phase mixes **admin** writes (`/api/v1/admin/**`, require `ROLE_ADMIN`) with **authenticated**
browsing (`/api/v1/products|categories|brands`, any logged-in user).

**Prerequisites:**

- Stack running, `export BASE=http://localhost:8080`.
- An **admin** token in `$ADMIN_TOKEN` — follow
  [Getting an admin token](README.md#getting-an-admin-token) first.
- A **customer** token in `$TOKEN` (any registered user — see [Phase 1](PHASE1-auth-user.md)).
  Used to demonstrate browsing and the `403` on admin endpoints.

---

## 1. Create a brand (admin)

`POST /api/v1/admin/brands`. `slug` is optional — derived from `name` when omitted.

```bash
BRAND=$(curl -s $BASE/api/v1/admin/brands \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{ "name": "Acme", "description": "Acme Corp", "logoUrl": "https://cdn.example.com/acme.png" }')
echo "$BRAND" | jq
export BRAND_ID=$(echo "$BRAND" | jq -r '.data.id')
```

**`201 Created`** with a `BrandResponse` (`id`, `name`, `slug`, `active`, …).
A duplicate slug → `409`.

---

## 2. Create a category (admin)

`POST /api/v1/admin/categories`. `parentCategoryId` is optional (for sub-categories).

```bash
CAT=$(curl -s $BASE/api/v1/admin/categories \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{ "name": "Laptops", "description": "Portable computers" }')
echo "$CAT" | jq
export CATEGORY_ID=$(echo "$CAT" | jq -r '.data.id')
```

**`201 Created`** with a `CategoryResponse`.

---

## 3. Create a product (admin)

`POST /api/v1/admin/products`. `categoryId` and `price` are required; `brandId`, `slug`,
`description` and `images` are optional. `currency` is a 3-letter ISO code.

```bash
PROD=$(curl -s $BASE/api/v1/admin/products \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{
    \"name\": \"UltraBook 14\",
    \"sku\": \"UB-14-2026\",
    \"description\": \"14-inch ultrabook\",
    \"price\": 1299.00,
    \"currency\": \"USD\",
    \"categoryId\": \"$CATEGORY_ID\",
    \"brandId\": \"$BRAND_ID\",
    \"images\": [ { \"url\": \"https://cdn.example.com/ub14.jpg\", \"altText\": \"Front\", \"primary\": true } ]
  }")
echo "$PROD" | jq
export PRODUCT_ID=$(echo "$PROD" | jq -r '.data.id')
```

**`201 Created`** with a `ProductResponse`. A duplicate slug or SKU → `409`; a non-positive price or
missing required field → `400`.

> **Admin authorization check.** Repeat this call with the **customer** token and you get `403`:
> ```bash
> curl -s -o /dev/null -w '%{http_code}\n' $BASE/api/v1/admin/products \
>   -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
>   -d "{\"name\":\"x\",\"sku\":\"x\",\"price\":1,\"currency\":\"USD\",\"categoryId\":\"$CATEGORY_ID\"}"
> # 403
> ```

---

## 4. Inventory is auto-seeded

Creating the product fired `ProductCreatedEvent`; `inventory` seeded a zero-stock record. Read it:

```bash
curl -s $BASE/api/v1/admin/inventory/$PRODUCT_ID -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

**`200 OK`:**

```json
{
  "success": true,
  "message": "Success",
  "data": { "productId": "…", "quantityOnHand": 0, "quantityReserved": 0, "quantityAvailable": 0 }
}
```

> Seeding is asynchronous (committed-event listener). A `404` immediately after creating the
> product just means it hasn't landed yet — wait a second and retry.

---

## 5. Browse the catalog (any authenticated user)

These use the **customer** token (`$TOKEN`) to show that ordinary users can browse.

### List products (paginated)

```bash
curl -s "$BASE/api/v1/products?page=0&size=20&sort=price,desc" \
  -H "Authorization: Bearer $TOKEN" | jq
```

`data` is the page envelope (`content`, `page`, `size`, `totalElements`, `totalPages`,
`first`, `last`). Only **active** products appear. Optional filters: `categoryId`, `brandId`,
`minPrice`, `maxPrice`.

```bash
curl -s "$BASE/api/v1/products?categoryId=$CATEGORY_ID&minPrice=500&maxPrice=2000" \
  -H "Authorization: Bearer $TOKEN" | jq '.data.content[].name'
```

### Get one product

```bash
curl -s $BASE/api/v1/products/$PRODUCT_ID -H "Authorization: Bearer $TOKEN" | jq
```

### Keyword search

`GET /api/v1/products/search` — matches `keyword` against name, description and SKU, with the same
filters/pagination as the list endpoint.

```bash
curl -s "$BASE/api/v1/products/search?keyword=ultrabook&sort=price,desc" \
  -H "Authorization: Bearer $TOKEN" | jq '.data.content[] | {name, price, sku}'
```

### Categories & brands

```bash
curl -s $BASE/api/v1/categories -H "Authorization: Bearer $TOKEN" | jq
curl -s $BASE/api/v1/categories/$CATEGORY_ID -H "Authorization: Bearer $TOKEN" | jq
curl -s $BASE/api/v1/brands -H "Authorization: Bearer $TOKEN" | jq
curl -s $BASE/api/v1/brands/$BRAND_ID -H "Authorization: Bearer $TOKEN" | jq
```

---

## 6. Set stock (admin)

`PUT /api/v1/admin/inventory/{productId}` sets the **absolute** on-hand quantity.

```bash
curl -s $BASE/api/v1/admin/inventory/$PRODUCT_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{ "quantityOnHand": 100, "reason": "Initial receipt" }' | jq
```

`200 OK` → `quantityOnHand: 100, quantityReserved: 0, quantityAvailable: 100`. Setting on-hand
**below** what's already reserved → `409`.

---

## 7. Reserve stock (admin)

`POST /api/v1/admin/inventory/reserve`. Decrements available, increments reserved.

```bash
RES=$(curl -s $BASE/api/v1/admin/inventory/reserve \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{ \"productId\": \"$PRODUCT_ID\", \"quantity\": 2, \"reference\": \"manual-test-1\" }")
echo "$RES" | jq
export RESERVATION_ID=$(echo "$RES" | jq -r '.data.reservationId')
```

**`200 OK`:** `ReservationResponse` (`reservationId`, `productId`, `quantity`, `status: "ACTIVE"`).
Re-read stock to see the effect:

```bash
curl -s $BASE/api/v1/admin/inventory/$PRODUCT_ID -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq '.data'
# quantityOnHand 100, quantityReserved 2, quantityAvailable 98
```

**Over-reservation → `409`** (asking for more than is available):

```bash
curl -s -o /dev/null -w '%{http_code}\n' $BASE/api/v1/admin/inventory/reserve \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d "{ \"productId\": \"$PRODUCT_ID\", \"quantity\": 99999 }"
# 409
```

---

## 8. Release stock (admin)

`POST /api/v1/admin/inventory/release` returns a reservation's units to available stock.

```bash
curl -s $BASE/api/v1/admin/inventory/release \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{ \"reservationId\": \"$RESERVATION_ID\" }" | jq
```

`200 OK`. Available goes back to 100. Releasing the same reservation again → `409`
(already released); an unknown `reservationId` → `404`.

---

## Endpoint summary

| Method | Path | Auth | Notes |
| ------ | ---- | ---- | ----- |
| POST | `/api/v1/admin/brands` | admin | 409 on dup slug |
| PUT/DELETE | `/api/v1/admin/brands/{brandId}` | admin | |
| POST | `/api/v1/admin/categories` | admin | optional `parentCategoryId` |
| PUT/DELETE | `/api/v1/admin/categories/{categoryId}` | admin | |
| POST | `/api/v1/admin/products` | admin | 409 dup slug/SKU; seeds inventory |
| PUT/DELETE | `/api/v1/admin/products/{productId}` | admin | |
| GET | `/api/v1/products` | bearer | paginated, filterable, active-only |
| GET | `/api/v1/products/search` | bearer | `keyword` + filters |
| GET | `/api/v1/products/{productId}` | bearer | |
| GET | `/api/v1/categories`, `/categories/{id}` | bearer | |
| GET | `/api/v1/brands`, `/brands/{id}` | bearer | |
| GET | `/api/v1/admin/inventory/{productId}` | admin | stock levels |
| PUT | `/api/v1/admin/inventory/{productId}` | admin | set absolute on-hand; 409 below reserved |
| POST | `/api/v1/admin/inventory/reserve` | admin | 409 if insufficient |
| POST | `/api/v1/admin/inventory/release` | admin | 404 unknown, 409 already released |

Next: [Phase 3 — Cart & Coupon](PHASE3-cart-coupon.md) — reuses `$PRODUCT_ID` (with stock) from
this phase.