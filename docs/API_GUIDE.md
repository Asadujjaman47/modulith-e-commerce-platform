# API_GUIDE.md

# E-Commerce Platform API Guide

Version: 1.0

Base URL:

/api/v1

Protocol:

HTTPS

Response Format:

JSON

Authentication:

JWT Bearer Token

---

# 1. API Design Principles

Rules:

* Use REST conventions.
* Use plural resource names.
* Use nouns, not verbs.
* Use HTTP methods correctly.
* Never expose JPA entities.
* Use DTOs only.
* Version APIs.
* Return consistent response structures.
* Support pagination on collection endpoints.

Examples:

Good:

GET /api/v1/products

GET /api/v1/orders/123

POST /api/v1/orders

Bad:

GET /api/v1/getProducts

POST /api/v1/createOrder

PUT /api/v1/updateProduct

---

# 2. Authentication

Header:

Authorization: Bearer <jwt-token>

Example:

Authorization: Bearer eyJhbGciOi...

Protected Endpoints:

All endpoints except:

POST /api/v1/auth/register

POST /api/v1/auth/login

POST /api/v1/auth/refresh

---

# 3. Content Type

Request:

Content-Type: application/json

Response:

Content-Type: application/json

---

# 4. Standard Success Response

{
"success": true,
"message": "Operation completed successfully",
"data": {}
}

---

# 5. Standard Error Response

{
"success": false,
"message": "Validation failed",
"errors": [
{
"field": "email",
"message": "Email is invalid"
}
]
}

---

# 6. HTTP Status Codes

200 OK

Successful retrieval

201 Created

Resource created

204 No Content

Resource deleted

400 Bad Request

Validation failure

401 Unauthorized

Missing or invalid token

403 Forbidden

Insufficient permission

404 Not Found

Resource does not exist

409 Conflict

Business rule violation

422 Unprocessable Entity

Domain validation failure

500 Internal Server Error

Unexpected error

---

# 7. Pagination Standard

All collection endpoints must support:

?page=0

&size=20

&sort=name,asc

Example:

GET /api/v1/products?page=0&size=20&sort=name,asc

---

Pagination Response

{
"success": true,
"data": {
"content": [],
"page": 0,
"size": 20,
"totalElements": 100,
"totalPages": 5,
"first": true,
"last": false
}
}

Default Page Size:

20

Maximum Page Size:

100

---

# 8. Sorting Standard

Single Sort

GET /api/v1/products?sort=name,asc

Descending

GET /api/v1/products?sort=price,desc

Multiple Sorts

GET /api/v1/products?sort=category,asc&sort=price,desc

---

# 9. Filtering Standard

Products

GET /api/v1/products

Query Parameters:

categoryId

brandId

minPrice

maxPrice

keyword

Example:

GET /api/v1/products?categoryId=1&minPrice=100&maxPrice=500

---

# 10. API Versioning

Current:

/api/v1

Future:

/api/v2

Breaking changes require a new version.

---

# 11. Auth APIs

Register

POST /api/v1/auth/register

Request

{
"email": "[john@example.com](mailto:john@example.com)",
"password": "Password123!",
"firstName": "John",
"lastName": "Doe"
}

Response

201 Created

---

Login

POST /api/v1/auth/login

Request

{
"email": "[john@example.com](mailto:john@example.com)",
"password": "Password123!"
}

Response

{
"success": true,
"data": {
"accessToken": "...",
"refreshToken": "...",
"expiresIn": 900,
"tokenType": "Bearer"
}
}

---

Refresh Token

POST /api/v1/auth/refresh

Request

{
"refreshToken": "..."
}

Response (tokens are rotated: the presented refresh token is revoked and a new pair issued)

{
"success": true,
"data": {
"accessToken": "...",
"refreshToken": "...",
"expiresIn": 900,
"tokenType": "Bearer"
}
}

---

Logout

POST /api/v1/auth/logout

Requires a valid access token (Authorization: Bearer ...).

Request

{
"refreshToken": "..."
}

Revokes the supplied refresh token. The short-lived access token expires naturally.

---

# 12. User APIs

Get Profile

GET /api/v1/users/me

Update Profile

PUT /api/v1/users/me

List Addresses

GET /api/v1/users/me/addresses

Create Address

POST /api/v1/users/me/addresses

Update Address

PUT /api/v1/users/me/addresses/{addressId}

Delete Address

DELETE /api/v1/users/me/addresses/{addressId}

---

# 13. Catalog APIs

List Products

GET /api/v1/products

Supports pagination (`page`, `size`, `sort`) and filters: `categoryId`, `brandId`, `minPrice`,
`maxPrice`. Returns the standard `PageResponse` envelope. Only active products are listed.

Get Product

GET /api/v1/products/{productId}

Search Products

GET /api/v1/products/search

Keyword search over product name, description and SKU via `keyword`, plus the same filters and
pagination as the list endpoint.

Example:

GET /api/v1/products/search?keyword=ultrabook&categoryId=...&minPrice=500&page=0&size=20&sort=price,desc

Create Product

POST /api/v1/admin/products

Update Product

PUT /api/v1/admin/products/{productId}

Delete Product

DELETE /api/v1/admin/products/{productId}

---

Categories

GET /api/v1/categories

GET /api/v1/categories/{categoryId}

POST /api/v1/admin/categories

PUT /api/v1/admin/categories/{categoryId}

DELETE /api/v1/admin/categories/{categoryId}

---

Brands

GET /api/v1/brands

GET /api/v1/brands/{brandId}

POST /api/v1/admin/brands

PUT /api/v1/admin/brands/{brandId}

DELETE /api/v1/admin/brands/{brandId}

---

# 14. Inventory APIs

All inventory endpoints require `ROLE_ADMIN`. Responses report `quantityOnHand`,
`quantityReserved` and derived `quantityAvailable`.

Get Stock

GET /api/v1/admin/inventory/{productId}

Update Stock (sets absolute on-hand quantity)

PUT /api/v1/admin/inventory/{productId}

Request

{
"quantityOnHand": 100,
"reason": "Initial receipt"
}

Reserve Stock

POST /api/v1/admin/inventory/reserve

Request

{
"productId": "...",
"quantity": 2,
"reference": "order-1"
}

Returns 409 when insufficient stock is available.

Release Stock

POST /api/v1/admin/inventory/release

Request

{
"reservationId": "..."
}

---

# 15. Cart APIs

All cart endpoints require authentication; the cart belongs to the customer resolved from the JWT.
Each line snapshots the product name and unit price at add time. Responses include the cart items
and the computed `subtotal`.

Get Cart

GET /api/v1/cart

Returns the customer's cart, creating an empty one on first access.

Add Item

POST /api/v1/cart/items

Request

{
"productId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
"quantity": 2
}

Adding an inactive or unknown product returns 409/404; exceeding available stock returns 409.

Update Item

PUT /api/v1/cart/items/{itemId}

Request

{
"quantity": 3
}

Remove Item

DELETE /api/v1/cart/items/{itemId}

Checkout

POST /api/v1/cart/checkout (planned — delivered with the order module)

---

# 16. Coupon APIs

Validate and apply operate on an order amount supplied in the request, so the coupon module stays
independent of cart/order. Both require authentication.

Validate Coupon

POST /api/v1/coupons/validate

Request

{
"code": "SAVE20",
"orderAmount": 150.00
}

Response

{
"success": true,
"data": {
"valid": true,
"code": "SAVE20",
"discountType": "PERCENTAGE",
"orderAmount": 150.00,
"discountAmount": 30.00,
"finalAmount": 120.00
}
}

An inactive, expired, usage-exhausted, or below-minimum coupon returns 409; an unknown code 404.

Apply Coupon

POST /api/v1/coupons/apply

Request

{
"code": "SAVE20",
"orderAmount": 150.00
}

Records a coupon usage for the authenticated customer, publishes `CouponAppliedEvent`, and returns
the discount and `finalAmount`.

Admin Create Coupon

POST /api/v1/admin/coupons

Request

{
"code": "SAVE20",
"description": "20% off orders over $100",
"discountType": "PERCENTAGE",
"discountValue": 20,
"minOrderAmount": 100.00,
"maxDiscountAmount": 50.00,
"validFrom": "2026-06-01T00:00:00Z",
"validUntil": "2026-12-31T23:59:59Z",
"usageLimit": 1000
}

`discountType` is `PERCENTAGE` or `FIXED_AMOUNT`. Requires `ROLE_ADMIN`.

---

# 17. Order APIs

All order endpoints require authentication. An order is placed from the authenticated customer's
current cart; the response is the standard envelope wrapping an `OrderResponse`.

Place Order

POST /api/v1/orders

Request (the contents come from the cart; pick a saved address and optionally a coupon)

{
"addressId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
"couponCode": "SAVE20"
}

Send an optional `Idempotency-Key` header so a retried request returns the original order instead of
creating a duplicate.

Response

201 Created

{
"success": true,
"message": "Order placed",
"data": {
"id": "...",
"orderNumber": "ORD-20260626-AB12CD",
"status": "PENDING",
"items": [ { "productId": "...", "productName": "UltraBook 14", "unitPrice": 1299.00, "quantity": 2, "lineTotal": 2598.00 } ],
"shippingAddress": { "line1": "221B Baker St", "city": "London", "postalCode": "NW1 6XE", "country": "GB" },
"currency": "USD",
"subtotal": 2598.00,
"couponCode": "SAVE20",
"discountAmount": 200.00,
"totalAmount": 2398.00,
"placedAt": "2026-06-26T10:15:00Z",
"cancelledAt": null
}
}

An empty cart, insufficient stock, or an invalid coupon → 409; an unknown address or coupon → 404.

Get Order

GET /api/v1/orders/{orderId}

Returns the order if it belongs to the caller, otherwise 404.

List Orders (history)

GET /api/v1/orders

Paginated (`page`, `size`, `sort`), with an optional `status` filter. Returns the standard
`PageResponse` of `OrderSummaryResponse`.

Cancel Order

POST /api/v1/orders/{orderId}/cancel

Cancels the caller's order while it is PENDING/PAID/PROCESSING (releases reserved stock); 409 if it
can no longer be cancelled.

---

Admin Order APIs (require ROLE_ADMIN)

List All Orders

GET /api/v1/admin/orders

Paginated, with an optional `status` filter.

Get Any Order

GET /api/v1/admin/orders/{orderId}

Update Order Status

PUT /api/v1/admin/orders/{orderId}/status

Request

{
"status": "PROCESSING"
}

Transitions are guarded: PENDING → PAID|PROCESSING|CANCELLED, PAID → PROCESSING|CANCELLED|REFUNDED,
PROCESSING → SHIPPED|CANCELLED, SHIPPED → DELIVERED, DELIVERED → REFUNDED. An illegal transition →
409. Reaching DELIVERED publishes `OrderCompletedEvent`.

---

Order Status Values

PENDING

PAID

PROCESSING

SHIPPED

DELIVERED

CANCELLED

REFUNDED

---

# 18. Payment APIs

Create Payment

POST /api/v1/payments

Get Payment

GET /api/v1/payments/{paymentId}

Refund Payment

POST /api/v1/payments/{paymentId}/refund

---

Payment Status Values

PENDING

SUCCESS

FAILED

REFUNDED

---

# 19. Shipment APIs

Create Shipment

POST /api/v1/admin/shipments

Track Shipment

GET /api/v1/shipments/{shipmentId}

Mark Delivered

POST /api/v1/admin/shipments/{shipmentId}/deliver

---

Shipment Status Values

CREATED

PICKED_UP

IN_TRANSIT

OUT_FOR_DELIVERY

DELIVERED

---

# 20. Review APIs

Create Review

POST /api/v1/products/{productId}/reviews

List Reviews

GET /api/v1/products/{productId}/reviews

Delete Review

DELETE /api/v1/reviews/{reviewId}

---

# 21. Reporting APIs

Admin Only

Sales Report

GET /api/v1/admin/reports/sales

Inventory Report

GET /api/v1/admin/reports/inventory

Customer Report

GET /api/v1/admin/reports/customers

---

# 22. OpenAPI Documentation

Swagger UI

/swagger-ui.html

OpenAPI JSON

/v3/api-docs

Every endpoint must contain:

* Summary
* Description
* Request Example
* Response Example
* Status Codes

---

# 23. Validation Rules

Email

Valid email format

Password

Minimum 8 characters

At least:

* 1 uppercase
* 1 lowercase
* 1 number
* 1 special character

Product Price

Greater than 0

Quantity

Greater than 0

---

# 24. Idempotency

Required For:

Payments

Order Creation

Refunds

Header:

Idempotency-Key

Example:

Idempotency-Key: 8b3d3b9c-21ab-4db4-b93d-f80f53a1d801

---

# 25. API Security Rules

Never expose:

* Passwords
* Password hashes
* Internal IDs
* Secrets
* API Keys

Sensitive fields must be omitted from responses.

---

# 26. Naming Conventions

Resources

Plural

Examples:

/products

/orders

/users

Path Variables

camelCase

Examples:

productId

orderId

Query Parameters

camelCase

Examples:

minPrice

maxPrice

categoryId

---

# 27. Deprecation Policy

Deprecated endpoints must:

* Remain functional for one major version
* Be documented in changelog
* Include deprecation notice in OpenAPI

Example:

Deprecated since v2.0

Removal planned for v3.0

---

# 28. API Definition of Done

An endpoint is complete only when:

* Request DTO created
* Response DTO created
* Validation implemented
* Business logic implemented
* Integration tests written
* OpenAPI documented
* Security configured
* Error handling implemented
* Logging added
* Reviewed and approved
