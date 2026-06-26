# Phase 5 — Payment & Shipment

Modules: **`payment`** and **`shipment`**.

When an order is placed, the payment module creates a **PENDING payment intent** for it
(consuming `OrderCreatedEvent`). The customer then **processes** the payment through a pluggable
gateway (the bundled `SimulatedPaymentGateway` approves every charge). A successful charge publishes
`PaymentCompletedEvent`, and two things happen after commit, each in its own transaction:

- the payment module marks the order **PAID** (via the order `spi`), and
- the shipment module **auto-creates a shipment** for the order and pushes it to **PROCESSING**.

An admin advances the shipment through `CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY →
DELIVERED`; delivery completes the order (**DELIVERED**). Admins can also **refund** a payment.

All order-status advancement is *pushed* into the order module through its `spi` — the order never
depends on payment/shipment — and those transitions are idempotent, so they tolerate the async,
unordered events that drive them.

**Prerequisites:**

- Stack running, `export BASE=http://localhost:8080`.
- A **customer** token in `$TOKEN` (see [Phase 1](PHASE1-auth-user.md)).
- A **placed order** in `$ORDER_ID` belonging to that customer (see [Phase 4](PHASE4-order.md) — you
  need an address, a stocked product and a cart item first).
- An **admin** token in `$ADMIN_TOKEN` (see [Getting an admin token](README.md#getting-an-admin-token))
  for the refund and shipment-management steps.

> There is exactly **one payment per order** and **one shipment per order**. The endpoints are
> idempotent accordingly: paying an already-paid order returns the original payment, and creating a
> shipment for an order that already has one returns the existing shipment.

---

## 1. See the pending payment intent

The intent is created asynchronously right after the order is placed. List your payments, filtered to
the order:

```bash
curl -s "$BASE/api/v1/payments?orderId=$ORDER_ID" \
  -H "Authorization: Bearer $TOKEN" | jq '.data.content'
```

```json
[
  { "id": "…", "orderId": "…", "status": "PENDING", "method": null,
    "amount": 300.00, "currency": "USD", "paidAt": null }
]
```

If the list is empty, retry after a moment — the intent is created on a post-commit event listener.
(You can pay even before it appears; step 2 gets-or-creates the intent.)

---

## 2. Process the payment

`POST /api/v1/payments`. The body picks the `orderId` and a `method` (`CARD`, `PAYPAL` or
`BANK_TRANSFER`). Send an optional `Idempotency-Key` header so a retried request is safe.

```bash
PAYMENT=$(curl -s $BASE/api/v1/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $(uuidgen)" \
  -d "{ \"orderId\": \"$ORDER_ID\", \"method\": \"CARD\" }")
echo "$PAYMENT" | jq
export PAYMENT_ID=$(echo "$PAYMENT" | jq -r '.data.id')
```

**`201 Created`** with the `PaymentResponse`:

```json
{
  "success": true,
  "message": "Payment processed",
  "data": {
    "id": "…",
    "orderId": "…",
    "status": "SUCCESS",
    "method": "CARD",
    "amount": 300.00,
    "currency": "USD",
    "gatewayReference": "SIM-CHG-…",
    "failureReason": null,
    "paidAt": "2026-06-26T…Z",
    "transactions": [
      { "type": "CHARGE", "succeeded": true, "amount": 300.00, "message": "Charge approved" }
    ]
  }
}
```

Paying again (with or without the same key) returns the same `SUCCESS` payment instead of charging
twice:

```bash
curl -s $BASE/api/v1/payments \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{ \"orderId\": \"$ORDER_ID\", \"method\": \"CARD\" }" \
  | jq '.data | {id, status}'
```

**Failure modes:** an unknown order or one that isn't yours → `404`; a cancelled/refunded or
zero-amount order → `409`.

---

## 3. The order is now PAID (then PROCESSING)

Marking the order paid and auto-creating the shipment happen on post-commit listeners. Check the
order — it will be `PAID`, and shortly after `PROCESSING` once the shipment is created:

```bash
curl -s $BASE/api/v1/orders/$ORDER_ID -H "Authorization: Bearer $TOKEN" | jq -r '.data.status'
# PAID   (then PROCESSING)
```

---

## 4. View payment status & history

Get a single payment (must be yours, else `404`):

```bash
curl -s $BASE/api/v1/payments/$PAYMENT_ID -H "Authorization: Bearer $TOKEN" \
  | jq '.data | {status, amount, gatewayReference, transactions}'
```

List all your payments (paginated; optional `orderId` filter):

```bash
curl -s "$BASE/api/v1/payments?page=0&size=20" -H "Authorization: Bearer $TOKEN" \
  | jq '.data | {totalElements, content: (.content | map({id, status, amount}))}'
```

---

## 5. Find/create the shipment (admin)

A shipment is auto-created when the payment completes. The admin **create** endpoint is idempotent —
it returns that shipment (assigning the carrier), or creates one if it doesn't exist yet:

```bash
SHIPMENT=$(curl -s $BASE/api/v1/admin/shipments \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{ \"orderId\": \"$ORDER_ID\", \"carrier\": \"DHL\" }")
echo "$SHIPMENT" | jq
export SHIPMENT_ID=$(echo "$SHIPMENT" | jq -r '.data.id')
```

**`201 Created`** with the `ShipmentResponse` (note the snapshotted delivery address and the initial
`CREATED` tracking record):

```json
{
  "data": {
    "id": "…",
    "orderId": "…",
    "status": "CREATED",
    "carrier": "DHL",
    "trackingNumber": "TRK-7F3K9Q2M",
    "deliveryAddress": { "line1": "221B Baker St", "city": "London", "postalCode": "NW1 6XE", "country": "GB" },
    "shippedAt": null,
    "deliveredAt": null,
    "estimatedDelivery": "2026-07-01T…Z",
    "trackingRecords": [ { "status": "CREATED", "note": "Shipment created", "occurredAt": "…" } ]
  }
}
```

An unpaid order → `409`; an unknown order → `404`.

---

## 6. Track the shipment (customer)

`GET /api/v1/shipments/{shipmentId}` — the owning customer sees the shipment and its full tracking
history (`404` if it isn't theirs):

```bash
curl -s $BASE/api/v1/shipments/$SHIPMENT_ID -H "Authorization: Bearer $TOKEN" \
  | jq '.data | {status, trackingNumber, trackingRecords}'
```

---

## 7. Advance the shipment (admin)

`PUT /api/v1/admin/shipments/{shipmentId}/status` walks the guarded state machine, appending a
tracking record each time. An illegal jump → `409`.

```bash
for s in PICKED_UP IN_TRANSIT OUT_FOR_DELIVERY; do
  curl -s -X PUT $BASE/api/v1/admin/shipments/$SHIPMENT_ID/status \
    -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
    -d "{ \"status\": \"$s\", \"location\": \"In transit\" }" \
    | jq -r '.data.status'
done
```

---

## 8. Confirm delivery (admin)

`POST /api/v1/admin/shipments/{shipmentId}/deliver` marks the shipment `DELIVERED`, publishes
`ShipmentDeliveredEvent`, and completes the order:

```bash
curl -s -X POST $BASE/api/v1/admin/shipments/$SHIPMENT_ID/deliver \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.data.status'
# DELIVERED
```

The order follows shortly after (async):

```bash
curl -s $BASE/api/v1/orders/$ORDER_ID -H "Authorization: Bearer $TOKEN" | jq -r '.data.status'
# DELIVERED
```

(Setting `"status": "DELIVERED"` via the status endpoint in step 7 does the same thing.)

---

## 9. Refund a payment (admin)

`POST /api/v1/admin/payments/{paymentId}/refund` refunds a successful payment (→ `REFUNDED`,
publishes `PaymentRefundedEvent`). It's idempotent — refunding again returns the refunded payment; a
non-successful payment → `409`.

```bash
curl -s -X POST $BASE/api/v1/admin/payments/$PAYMENT_ID/refund \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.data | {status, refundedAt}'
# { "status": "REFUNDED", "refundedAt": "…" }
```

Admins can also fetch any payment: `GET /api/v1/admin/payments/{paymentId}`.

---

## 10. Authorization checks

The `/api/v1/admin/**` payment and shipment endpoints require `ROLE_ADMIN` — a customer token gets
`403`:

```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  -X POST $BASE/api/v1/admin/payments/$PAYMENT_ID/refund -H "Authorization: Bearer $TOKEN"
# 403

curl -s -o /dev/null -w '%{http_code}\n' \
  -X POST $BASE/api/v1/admin/shipments -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d "{ \"orderId\": \"$ORDER_ID\", \"carrier\": \"DHL\" }"
# 403
```

---

## Status reference

**Payment:** `PENDING → SUCCESS | FAILED` · `FAILED → PENDING` (retry) · `SUCCESS → REFUNDED`

**Shipment:** `CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED`

**Order (driven by this phase):** `PENDING → PAID` (payment) `→ PROCESSING` (shipment created)
`→ SHIPPED → DELIVERED` (shipment delivered).
