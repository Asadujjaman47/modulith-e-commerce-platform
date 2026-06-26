# Phase 4 — Order

Module: **`order`** (the order lifecycle). An order is placed from a snapshot of the customer's
cart: the order reads the cart, the chosen shipping address and (optionally) a coupon discount, then
persists itself as `PENDING`. After it commits, the order module drives the downstream side effects —
**reserving stock**, **clearing the cart** and **recording coupon usage** — each in its own
transaction. Cancelling an order **releases** its reserved stock. An admin can walk an order through
its status lifecycle.

**Prerequisites:**

- Stack running, `export BASE=http://localhost:8080`.
- A **customer** token in `$TOKEN` (see [Phase 1](PHASE1-auth-user.md)). The order belongs to the
  customer resolved from this JWT.
- A **saved address** for that customer in `$ADDRESS_ID` (see
  [Phase 1 → addresses](PHASE1-auth-user.md)) — the order ships to it and snapshots it.
- An **active product with stock** in `$PRODUCT_ID` and **an item in the cart** (see
  [Phase 2](PHASE2-catalog-inventory.md) and [Phase 3](PHASE3-cart-coupon.md)).
- An **admin** token in `$ADMIN_TOKEN` for the status-lifecycle steps
  (see [Getting an admin token](README.md#getting-an-admin-token)).

> The order is built from whatever is in the cart **at placement time**, so add your items first
> (Phase 3, step 2). Placing the order empties the cart.

---

## 1. Place an order

`POST /api/v1/orders`. The body picks the shipping `addressId` and may include a `couponCode`. Pass
an optional `Idempotency-Key` header so a retried request returns the original order instead of
creating a duplicate.

```bash
ORDER=$(curl -s $BASE/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $(uuidgen)" \
  -d "{ \"addressId\": \"$ADDRESS_ID\" }")
echo "$ORDER" | jq
export ORDER_ID=$(echo "$ORDER" | jq -r '.data.id')
```

**`201 Created`** with the `OrderResponse`:

```json
{
  "data": {
    "id": "…",
    "orderNumber": "ORD-20260626-AB12CD",
    "customerId": "…",
    "status": "PENDING",
    "items": [
      { "id": "…", "productId": "…", "productName": "UltraBook 14",
        "unitPrice": 1299.00, "quantity": 2, "lineTotal": 2598.00 }
    ],
    "shippingAddress": {
      "label": "Home", "line1": "221B Baker St", "line2": null,
      "city": "London", "state": null, "postalCode": "NW1 6XE", "country": "GB"
    },
    "currency": "USD",
    "subtotal": 2598.00,
    "couponCode": null,
    "discountAmount": 0.00,
    "totalAmount": 2598.00,
    "placedAt": "2026-06-26T…Z",
    "cancelledAt": null
  }
}
```

To apply a coupon, add `"couponCode"` (the discount is computed from a read-only coupon quote and
recorded against the order once it's placed):

```bash
curl -s $BASE/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{ \"addressId\": \"$ADDRESS_ID\", \"couponCode\": \"SAVE20\" }" \
  | jq '.data | {subtotal, couponCode, discountAmount, totalAmount}'
```

Failure modes:

- Empty cart → `409`.
- Unknown address (or not owned by the customer) → `404`.
- A line that exceeds available stock → `409`.
- Unknown coupon → `404`; inactive/expired/exhausted/below-minimum coupon → `409`.

```bash
# 409 — nothing in the cart
curl -s -o /dev/null -w '%{http_code}\n' $BASE/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{ \"addressId\": \"$ADDRESS_ID\" }"
# 409  (after the cart has been emptied by a prior order)
```

## 2. Watch the side effects (async)

Placing the order publishes `OrderCreatedEvent`; the order module then reserves stock, clears the
cart and records any coupon usage on committed-event listeners a moment later.

```bash
# Cart is now empty
curl -s $BASE/api/v1/cart -H "Authorization: Bearer $TOKEN" \
  | jq '.data | {items: .items | length, subtotal}'
# { "items": 0, "subtotal": 0 }

# Stock for the product is now reserved (admin view)
curl -s $BASE/api/v1/admin/inventory/$PRODUCT_ID -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq '.data | {quantityOnHand, quantityReserved, quantityAvailable}'
```

> If a read looks stale immediately after placing, wait a second and retry — the side effects run
> just after the place-order transaction commits.

## 3. Get one order

`GET /api/v1/orders/{orderId}` — returns the full order, but only to its owner.

```bash
curl -s $BASE/api/v1/orders/$ORDER_ID -H "Authorization: Bearer $TOKEN" | jq '.data.status'
```

Another customer's token (or an unknown id) → `404`.

## 4. List my orders (history)

`GET /api/v1/orders` — the caller's orders as summaries, newest-first by default. Supports
pagination/sorting and an optional `status` filter.

```bash
curl -s "$BASE/api/v1/orders?page=0&size=20&sort=placedAt,desc" \
  -H "Authorization: Bearer $TOKEN" | jq '.data | {totalElements, content: .content | length}'

# Filter by status
curl -s "$BASE/api/v1/orders?status=PENDING" -H "Authorization: Bearer $TOKEN" | jq '.data.content'
```

Each entry is an `OrderSummaryResponse`:

```json
{ "id": "…", "orderNumber": "ORD-20260626-AB12CD", "status": "PENDING",
  "itemCount": 1, "totalAmount": 2598.00, "placedAt": "2026-06-26T…Z" }
```

(`itemCount` is the number of distinct line items, not the total quantity.)

## 5. Cancel an order

`POST /api/v1/orders/{orderId}/cancel` — allowed while the order is `PENDING`, `PAID` or
`PROCESSING`. Cancelling publishes `OrderCancelledEvent`, which releases the reserved stock.

```bash
curl -s -X POST $BASE/api/v1/orders/$ORDER_ID/cancel \
  -H "Authorization: Bearer $TOKEN" | jq '.data | {status, cancelledAt}'
# status: "CANCELLED"

# Reserved stock is released again (async)
curl -s $BASE/api/v1/admin/inventory/$PRODUCT_ID -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq '.data.quantityReserved'
```

Cancelling an order that is already `SHIPPED`/`DELIVERED`/`CANCELLED` → `409`.

---

## Admin order management

### 6. List / get any order (admin)

```bash
curl -s "$BASE/api/v1/admin/orders?status=PENDING" -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq '.data | {totalElements}'
curl -s $BASE/api/v1/admin/orders/$ORDER_ID -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.data.status'
```

### 7. Update order status (admin)

`PUT /api/v1/admin/orders/{orderId}/status` transitions the order, enforcing the allowed-transition
rules. Reaching `DELIVERED` publishes `OrderCompletedEvent`; an admin `CANCELLED` releases stock like
a customer cancel.

```bash
curl -s -X PUT $BASE/api/v1/admin/orders/$ORDER_ID/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{ "status": "PROCESSING" }' | jq '.data.status'
```

Allowed transitions:

```
PENDING    → PAID | PROCESSING | CANCELLED
PAID       → PROCESSING | CANCELLED | REFUNDED
PROCESSING → SHIPPED | CANCELLED
SHIPPED    → DELIVERED
DELIVERED  → REFUNDED
```

An illegal transition (e.g. `PENDING → DELIVERED`) → `409`. A non-admin token on any
`/api/v1/admin/orders/**` endpoint → `403`.

---

## Endpoint summary

| Method | Path | Auth | Notes |
| ------ | ---- | ---- | ----- |
| POST | `/api/v1/orders` | bearer | 201; `Idempotency-Key` optional; 404 address/coupon, 409 empty cart/stock/coupon |
| GET | `/api/v1/orders` | bearer | own history; paginated; `?status=` filter |
| GET | `/api/v1/orders/{orderId}` | bearer | own order; 404 otherwise |
| POST | `/api/v1/orders/{orderId}/cancel` | bearer | 200; 409 if not cancellable |
| GET | `/api/v1/admin/orders` | admin | all orders; paginated; `?status=` filter |
| GET | `/api/v1/admin/orders/{orderId}` | admin | any order |
| PUT | `/api/v1/admin/orders/{orderId}/status` | admin | guarded transition; 409 illegal |

That's the order lifecycle through Phase 4. Phases 5+ (payment, shipment, notification, …) will hang
off the `OrderCreatedEvent` / `OrderCompletedEvent` published here.
