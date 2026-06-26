# Phase 7 — Reporting & Audit

Modules: **`reporting`** and **`audit`**.

Both are **read-only, admin-only** modules built entirely from events published by the other modules —
there is nothing to "create" directly. You populate them by exercising the earlier phases, then read the
results back.

The **reporting** module listens for `OrderCreatedEvent` and records two immutable projections per
order: a sales fact (one per order) and product facts (one per line). It then aggregates these on
demand into a **sales report** (orders/units/revenue/discounts per day) and a **product report** (top
products by units sold). Order events carry no unit price, so product reports cover **units and order
counts**, not per-product revenue.

The **audit** module listens for **every** business event and appends one immutable row to
`audit_logs`. You can **search** the trail and view a **per-user activity** timeline.

**Prerequisites:**

- Stack running, `export BASE=http://localhost:8080`.
- An **admin** token in `$ADMIN_TOKEN` (see [Getting an admin token](README.md#getting-an-admin-token)).
  All Phase 7 endpoints are `ROLE_ADMIN`-only — a customer token gets `403`.
- **Some history to report on:** place at least one order (see [Phase 4](PHASE4-order.md)). Each placed
  order feeds both modules. Registering/logging in, creating products, paying, shipping, reviewing, etc.
  all add audit rows too.

All endpoints return the standard envelope; the collection endpoints use the standard page envelope
(see [Pagination & sorting](README.md#pagination--sorting)).

---

## Part A — Reporting

### 1. Sales report

`from` and `to` are inclusive `yyyy-MM-dd` dates (UTC). Orders are dated by the day they were placed.

```bash
export FROM=$(date -u +%F)        # today, UTC
export TO=$(date -u +%F)

curl -s "$BASE/api/v1/admin/reports/sales?from=$FROM&to=$TO" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "from": "2026-06-26",
    "to": "2026-06-26",
    "orderCount": 3,
    "unitsSold": 11,
    "revenue": 449.70,
    "discountTotal": 30.00,
    "daily": [
      { "date": "2026-06-26", "orderCount": 3, "unitsSold": 11, "revenue": 449.70, "discountTotal": 30.00 }
    ]
  }
}
```

`revenue` is the sum of order totals charged (net of discount); `discountTotal` is reported alongside.
An **inverted window** (`from` after `to`) returns **`409`**.

### 2. Product report

Top products by units sold over the window, paginated (standard page envelope; ordering is fixed to
units-sold descending):

```bash
curl -s "$BASE/api/v1/admin/reports/products?from=$FROM&to=$TO&page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      { "productId": "a1b2…", "unitsSold": 7, "orderCount": 3 },
      { "productId": "c3d4…", "unitsSold": 4, "orderCount": 1 }
    ],
    "page": 0, "size": 20, "totalElements": 2, "totalPages": 1, "first": true, "last": true
  }
}
```

> **Async:** the projection is written on a committed-event listener a moment after an order is placed.
> If a report doesn't reflect a just-placed order, wait a second and retry.

---

## Part B — Audit

### 3. Search the audit trail

All filters are optional and combine with AND; results are most-recent first.

| Param | Meaning |
| ----- | ------- |
| `category` | functional area: `AUTH`, `USER`, `CATALOG`, `INVENTORY`, `COUPON`, `ORDER`, `PAYMENT`, `SHIPMENT`, `REVIEW`, `NOTIFICATION` |
| `eventType` | exact event name, e.g. `OrderCreated`, `PaymentCompleted` |
| `entityId` | the affected entity's id (e.g. an order id) |
| `actorId` | the acting customer's id |
| `from` / `to` | ISO-8601 instants bounding `occurredAt` |

```bash
# Everything in the ORDER area
curl -s "$BASE/api/v1/admin/audit-logs?category=ORDER&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq

# A single order's trail
curl -s "$BASE/api/v1/admin/audit-logs?entityId=$ORDER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "…",
        "category": "ORDER",
        "eventType": "OrderCreated",
        "action": "CREATE",
        "entityType": "Order",
        "entityId": "…",
        "actorId": "…",
        "description": "Order ORD-1001 placed; total 49.99",
        "occurredAt": "2026-06-26T09:15:42.123Z"
      }
    ],
    "page": 0, "size": 20, "totalElements": 1, "totalPages": 1, "first": true, "last": true
  }
}
```

### 4. A user's activity timeline

`{userId}` is the customer's auth user id (which is also the `actorId` on their events):

```bash
curl -s "$BASE/api/v1/admin/audit-logs/activity/$USER_ID?size=50" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

Returns the same page envelope, filtered to that actor, most-recent first.

### 5. Admin-only

Any of the above with a **customer** token returns `403`:

```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  "$BASE/api/v1/admin/audit-logs" -H "Authorization: Bearer $TOKEN"
# 403
```
