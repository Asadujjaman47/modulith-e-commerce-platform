# Phase 3 — Cart & Coupon

Modules: **`cart`** (a per-customer shopping cart; items snapshot the product name + unit price at
add time and are checked against live inventory) and **`coupon`** (admin-created discount codes you
can validate/apply against an order amount). The two modules are independent — coupon math runs on
an `orderAmount` you pass in, not on the cart.

**Prerequisites:**

- Stack running, `export BASE=http://localhost:8080`.
- A **customer** token in `$TOKEN` (see [Phase 1](PHASE1-auth-user.md)). The cart belongs to the
  customer resolved from this JWT.
- An **active product with stock** in `$PRODUCT_ID`. The easiest path is to run
  [Phase 2](PHASE2-catalog-inventory.md) first (it leaves `$PRODUCT_ID` with on-hand 100). If you
  jumped straight here, create a product and set its stock per Phase 2 steps 3 and 6.
- An **admin** token in `$ADMIN_TOKEN` for the coupon-creation step
  (see [Getting an admin token](README.md#getting-an-admin-token)).

---

## Cart

### 1. Get (or create) the cart

`GET /api/v1/cart` — returns the caller's cart, creating an empty one on first access.

```bash
curl -s $BASE/api/v1/cart -H "Authorization: Bearer $TOKEN" | jq
```

**`200 OK`:**

```json
{
  "success": true,
  "message": "Success",
  "data": { "id": "…", "customerId": "…", "items": [], "subtotal": 0 }
}
```

### 2. Add an item

`POST /api/v1/cart/items`. Snapshots the product's current name and unit price; checks the
requested quantity against available inventory.

```bash
ADD=$(curl -s $BASE/api/v1/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{ \"productId\": \"$PRODUCT_ID\", \"quantity\": 2 }")
echo "$ADD" | jq
export ITEM_ID=$(echo "$ADD" | jq -r '.data.items[0].id')
```

**`201 Created`** with the updated `CartResponse`:

```json
{
  "data": {
    "id": "…", "customerId": "…",
    "items": [
      { "id": "…", "productId": "…", "productName": "UltraBook 14",
        "unitPrice": 1299.00, "quantity": 2, "lineTotal": 2598.00 }
    ],
    "subtotal": 2598.00
  }
}
```

Failure modes:

- Unknown product → `404`.
- Inactive product, or quantity beyond available stock → `409`.

```bash
# 409 — more than is in stock
curl -s -o /dev/null -w '%{http_code}\n' $BASE/api/v1/cart/items \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{ \"productId\": \"$PRODUCT_ID\", \"quantity\": 99999 }"
# 409
```

### 3. Update item quantity

`PUT /api/v1/cart/items/{itemId}` sets the **absolute** quantity (also re-checked against stock).

```bash
curl -s $BASE/api/v1/cart/items/$ITEM_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{ "quantity": 3 }' | jq '.data | {subtotal, items: .items | length}'
```

`200 OK` with the recomputed cart. Unknown `itemId` → `404`; exceeding stock → `409`.

### 4. Remove item

`DELETE /api/v1/cart/items/{itemId}` — returns the updated cart (`200`, not `204`).

```bash
curl -s -X DELETE $BASE/api/v1/cart/items/$ITEM_ID \
  -H "Authorization: Bearer $TOKEN" | jq '.data'
# items: [], subtotal: 0
```

> Checkout (`POST /api/v1/cart/checkout`) is intentionally **not** implemented yet — it's delivered
> with the order module in Phase 4.

---

## Coupon

### 5. Create a coupon (admin)

`POST /api/v1/admin/coupons`. `discountType` is `PERCENTAGE` or `FIXED_AMOUNT`. For a percentage
coupon, `maxDiscountAmount` caps the discount. `validFrom`/`validUntil` are ISO-8601 instants.

```bash
COUPON=$(curl -s $BASE/api/v1/admin/coupons \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "code": "SAVE20",
    "description": "20% off orders over $100",
    "discountType": "PERCENTAGE",
    "discountValue": 20,
    "minOrderAmount": 100.00,
    "maxDiscountAmount": 50.00,
    "validFrom":  "2026-01-01T00:00:00Z",
    "validUntil": "2026-12-31T23:59:59Z",
    "usageLimit": 1000
  }')
echo "$COUPON" | jq
```

**`201 Created`** with a `CouponResponse` (includes `timesUsed: 0`, `active: true`). A duplicate
code → `409`. Creating it with the **customer** token → `403`.

### 6. Validate a coupon (customer)

`POST /api/v1/coupons/validate` — computes the discount for a given `orderAmount` **without**
recording a use.

```bash
curl -s $BASE/api/v1/coupons/validate \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{ "code": "SAVE20", "orderAmount": 150.00 }' | jq
```

**`200 OK`:**

```json
{
  "data": {
    "valid": true,
    "code": "SAVE20",
    "discountType": "PERCENTAGE",
    "orderAmount": 150.00,
    "discountAmount": 30.00,
    "finalAmount": 120.00
  }
}
```

(20% of 150 = 30, under the 50 cap.) Failure modes:

```bash
# 404 — unknown code
curl -s -o /dev/null -w '%{http_code}\n' $BASE/api/v1/coupons/validate \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{ "code": "NOPE", "orderAmount": 150.00 }'
# 404

# 409 — below the coupon's minimum order amount
curl -s -o /dev/null -w '%{http_code}\n' $BASE/api/v1/coupons/validate \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{ "code": "SAVE20", "orderAmount": 50.00 }'
# 409
```

An inactive, expired or usage-exhausted coupon also returns `409`.

### 7. Apply a coupon (customer)

`POST /api/v1/coupons/apply` — same calculation, but it **records a usage** for the authenticated
customer (incrementing `timesUsed` toward `usageLimit`) and publishes `CouponAppliedEvent`.

```bash
curl -s $BASE/api/v1/coupons/apply \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{ "code": "SAVE20", "orderAmount": 150.00 }' | jq
```

**`200 OK`:**

```json
{
  "data": {
    "couponId": "…",
    "code": "SAVE20",
    "orderAmount": 150.00,
    "discountAmount": 30.00,
    "finalAmount": 120.00
  }
}
```

Same `404`/`409` rules as validate. Once usage hits the `usageLimit`, further applies → `409`.

---

## Endpoint summary

| Method | Path | Auth | Notes |
| ------ | ---- | ---- | ----- |
| GET | `/api/v1/cart` | bearer | creates an empty cart on first access |
| POST | `/api/v1/cart/items` | bearer | 201; 404 unknown product, 409 inactive/over-stock |
| PUT | `/api/v1/cart/items/{itemId}` | bearer | absolute quantity; 404/409 |
| DELETE | `/api/v1/cart/items/{itemId}` | bearer | returns updated cart (200) |
| POST | `/api/v1/admin/coupons` | admin | 409 on duplicate code |
| POST | `/api/v1/coupons/validate` | bearer | no usage recorded; 404/409 |
| POST | `/api/v1/coupons/apply` | bearer | records usage + event; 404/409 |

That's everything implemented through Phase 3. Phase 4 (order) will wire cart checkout and coupon
application into the order lifecycle.