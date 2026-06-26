# Phase 6 — Notification & Review

Modules: **`notification`** and **`review`**.

The **notification** module is event-driven and has **no HTTP API**. It listens for business events and
sends an email for each, recording every attempt in `notification_logs`:

| Event (source) | Email |
| -------------- | ----- |
| `UserRegisteredEvent` (auth) | Welcome |
| `OrderCreatedEvent` (order) | Order confirmation |
| `PaymentCompletedEvent` (payment) | Payment received |
| `ShipmentCreatedEvent` (shipment) | Your order has shipped |
| `ShipmentDeliveredEvent` (shipment) | Your order has been delivered |

It keeps a local copy of each recipient's email (seeded from `UserRegisteredEvent`) so it never depends
on the `user` module. Delivery failures are logged as `FAILED` and never break the triggering flow.

The **review** module lets a purchase-eligible customer rate a product (1–5) and comment. A customer
becomes eligible after **at least one delivered order** (driven by `OrderCompletedEvent`) and may review
a given product **once**. It maintains an aggregate rating per product and supports admin moderation.

**Prerequisites:**

- Stack running, `export BASE=http://localhost:8080`. **Include Mailpit** so emails have somewhere to go:
  ```bash
  docker compose up -d postgres redis mailpit
  ./mvnw spring-boot:run
  ```
  (or `docker compose up -d --build` for the whole stack). Mailpit's web UI is at
  **http://localhost:8025**.
- A **customer** token in `$TOKEN` (see [Phase 1](PHASE1-auth-user.md)).
- An **admin** token in `$ADMIN_TOKEN` (see [Getting an admin token](README.md#getting-an-admin-token)).
- For reviews you need a **product id** in `$PRODUCT_ID` (see [Phase 2](PHASE2-catalog-inventory.md))
  and, to pass the purchase gate, a **delivered order** for the customer (see
  [Phase 4](PHASE4-order.md) + [Phase 5](PHASE5-payment-shipment.md) — place an order, pay it, and have
  the admin advance the shipment to **DELIVERED**).

---

## Part A — Notification (verify via Mailpit)

There are no endpoints to call — you trigger emails by exercising the other modules and **read them in
Mailpit**.

### 1. Welcome email on registration

Register a brand-new user (Phase 1). Within a second or two an email appears in Mailpit:

```bash
curl -s $BASE/api/v1/auth/register -H 'Content-Type: application/json' -d '{
  "email": "shopper@example.com",
  "password": "Password123!",
  "firstName": "Sam",
  "lastName": "Shopper"
}' | jq
```

Open **http://localhost:8025** — you should see **“Welcome to our store”** addressed to
`shopper@example.com`.

### 2. Lifecycle emails

As you run the Phase 4/5 flows for this customer, more emails arrive in Mailpit:

- placing an order → **“Order ORD-… confirmed”**
- paying it → **“Payment received”**
- shipment auto-created → **“Your order has shipped”**
- admin marks it delivered → **“Your order has been delivered”**

### 3. Inspect the delivery log (optional, in Postgres)

Every attempt is recorded. To see them:

```bash
docker exec -i ecommerce-postgres psql -U ecommerce -d ecommerce \
  -c "SELECT type, recipient, status, sent_at FROM notification_logs ORDER BY created_at DESC LIMIT 10;"
```

`status` is `SENT` (or `FAILED` if Mailpit/SMTP was unreachable — the business flow still succeeds).

---

## Part B — Review

Set the product you'll review:

```bash
export PRODUCT_ID=<a product id from Phase 2>
```

### 4. Create a review

`POST /api/v1/products/{productId}/reviews`. `rating` (1–5) is required; `title` and `comment` are
optional.

```bash
curl -s $BASE/api/v1/products/$PRODUCT_ID/reviews \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"rating":5,"title":"Excellent","comment":"Exactly as described, fast shipping."}' | jq
```

```json
{
  "success": true,
  "message": "Review created",
  "data": {
    "id": "…", "productId": "…", "customerId": "…",
    "authorName": "Sam Shopper", "rating": 5,
    "title": "Excellent", "comment": "Exactly as described, fast shipping.",
    "status": "PUBLISHED", "createdAt": "…"
  }
}
```

Capture the id for later:

```bash
export REVIEW_ID=$(curl -s $BASE/api/v1/products/$PRODUCT_ID/reviews \
  -H "Authorization: Bearer $TOKEN" | jq -r '.data.content[0].id')
echo $REVIEW_ID
```

**Expected failures:**

- **409** if the customer has **no delivered order** yet — *“You can only review products after
  completing an order.”*
- **409** on a **second** review of the same product — *“You have already reviewed this product.”*
- **409** if the product is **inactive**; **404** if the product id is unknown.
- **400** if `rating` is missing or outside 1–5.

### 5. List a product's reviews (paginated)

`GET /api/v1/products/{productId}/reviews` — standard page envelope, `?page=&size=&sort=`.

```bash
curl -s "$BASE/api/v1/products/$PRODUCT_ID/reviews?size=10" \
  -H "Authorization: Bearer $TOKEN" | jq '.data'
```

### 6. Product rating summary

`GET /api/v1/products/{productId}/reviews/summary`.

```bash
curl -s $BASE/api/v1/products/$PRODUCT_ID/reviews/summary \
  -H "Authorization: Bearer $TOKEN" | jq '.data'
```

```json
{ "productId": "…", "averageRating": 5.00, "reviewCount": 1 }
```

### 7. Moderate a review (admin)

`PUT /api/v1/admin/reviews/{reviewId}/status` — hide a published review or restore a hidden one. Hidden
reviews drop out of listings and the rating summary; restoring adds them back.

```bash
# hide
curl -s -X PUT $BASE/api/v1/admin/reviews/$REVIEW_ID/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"status":"HIDDEN"}' | jq '.data.status'   # "HIDDEN"

# the summary now reports zero
curl -s $BASE/api/v1/products/$PRODUCT_ID/reviews/summary \
  -H "Authorization: Bearer $TOKEN" | jq '.data.reviewCount'   # 0

# restore
curl -s -X PUT $BASE/api/v1/admin/reviews/$REVIEW_ID/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"status":"PUBLISHED"}' | jq '.data.status'   # "PUBLISHED"
```

A non-admin calling this endpoint gets **403**.

### 8. Delete your own review

`DELETE /api/v1/reviews/{reviewId}` — removes the caller's review and updates the rating.

```bash
curl -s -i -X DELETE $BASE/api/v1/reviews/$REVIEW_ID \
  -H "Authorization: Bearer $TOKEN"
# 200; deleting someone else's (or a missing) review → 404
```

---

## Recap

| # | Call | Result |
| - | ---- | ------ |
| 1 | Register a user | Welcome email in Mailpit |
| 2 | Order → pay → deliver | Confirmation / payment / shipped / delivered emails |
| 4 | `POST /products/{id}/reviews` | Creates a review (purchase-gated, one per product) |
| 5 | `GET /products/{id}/reviews` | Paginated published reviews |
| 6 | `GET /products/{id}/reviews/summary` | Aggregate `averageRating` + `reviewCount` |
| 7 | `PUT /admin/reviews/{id}/status` | Admin hide/restore (rating kept in sync) |
| 8 | `DELETE /reviews/{id}` | Owner deletes their review |
