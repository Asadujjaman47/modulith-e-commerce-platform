# Current Milestone

Phase 6 — Notification (next)

## Completed

### Phase 0 — Foundation (merged to `main`, PR #1)

- Spring Boot 3.5.15 + Spring Modulith 1.4.12 project skeleton (Maven, Java 21)
- Package-by-module structure with 15 module boundaries
- `common` module: AuditableEntity, ApiResponse/ErrorResponse envelope, exceptions, GlobalExceptionHandler
- `config` module: JPA auditing, Redis, OpenAPI, baseline Security (stateless, BCrypt)
- PostgreSQL + Flyway (V1__init.sql baseline)
- Redis integration
- OpenAPI / Swagger UI
- Actuator (health, info, prometheus)
- Dockerfile + docker-compose (postgres, redis, mailpit, app, prometheus, grafana)
- Tests: Modulith verification, Testcontainers context load, unit tests

### Phase 1 — Authentication & User Management

- `auth` module
  - Aggregates: `UserCredential` (auth_users), `RefreshToken` (refresh_tokens)
  - Use cases: register, login, refresh (with rotation), logout (refresh-token revocation)
  - JWT access tokens (15 min) + opaque refresh tokens (7 days, SHA-256 hashed in Postgres)
  - `JwtAuthenticationFilter` + JWT-protected `SecurityConfig` (owned by auth); `@EnableMethodSecurity`
  - Custom 401/403 JSON responses via the standard error envelope
  - Events published: `UserRegisteredEvent`, `UserLoggedInEvent` (exposed as a named interface)
- `user` module
  - Aggregates: `Customer` (customers), `Address` (customer_addresses)
  - Profile management (`GET/PUT /api/v1/users/me`) and address CRUD (`/api/v1/users/me/addresses`)
  - MapStruct mappers (`CustomerMapper`, `AddressMapper`)
  - Consumes `UserRegisteredEvent` via `@ApplicationModuleListener` to create the customer profile
  - Events published: `CustomerCreatedEvent`, `CustomerUpdatedEvent`, `AddressAddedEvent`
- Flyway: `V2__auth.sql`, `V3__user.sql`
- JWT dependency (jjwt 0.12.6) + `app.jwt.*` configuration (env-overridable `JWT_SECRET`)
- Tests: 42 passing — use-case unit tests (Mockito), JWT provider tests, full Testcontainers
  integration test (register → async customer creation → login → profile/address → refresh → logout),
  Modulith verification

### Phase 2 — Catalog & Inventory

- `catalog` module
  - Aggregates: `Category` (categories), `Brand` (brands), `Product` (products) with `ProductImage`
    children (product_images)
  - Use cases: create/update/get/delete/search products; manage categories & brands
  - Search via JPA `Specification`s: keyword (name/description/sku), category, brand and price-range
    filters, with pagination + multi-sort (`PageResponse` envelope, default size 20, max 100)
  - Public (authenticated) browsing: `GET /api/v1/products`, `/products/{id}`, `/products/search`,
    `/categories`, `/categories/{id}`, `/brands`, `/brands/{id}`
  - Admin (`ROLE_ADMIN`, `@PreAuthorize`): `/api/v1/admin/products|categories|brands` CRUD
  - MapStruct mappers (`ProductMapper`, `CategoryMapper`, `BrandMapper`)
  - Events published (named interface): `ProductCreatedEvent`, `ProductUpdatedEvent`,
    `ProductDeletedEvent`
- `inventory` module
  - Aggregates: `Inventory` (inventory), `StockReservation` (stock_reservations),
    `InventoryTransaction` (inventory_transactions, append-only ledger)
  - Use cases: get/update stock, reserve stock, release stock (availability invariant enforced in
    the domain; over-reservation → HTTP 409)
  - Admin API (`ROLE_ADMIN`): `GET/PUT /api/v1/admin/inventory/{productId}`,
    `POST /api/v1/admin/inventory/reserve`, `POST /api/v1/admin/inventory/release`
  - Consumes catalog `ProductCreatedEvent` (`@ApplicationModuleListener`) to seed a zero-stock
    inventory record — the only `inventory -> catalog` coupling
  - Events published (named interface): `StockReservedEvent`, `StockReleasedEvent`,
    `StockUpdatedEvent`
  - Note: `OrderCreatedEvent` / `OrderCancelledEvent` consumers are deferred until the `order`
    module exists; reservation/release are driven through the admin API for now
- `common`: shared `PageResponse<T>` pagination envelope; `spring.data.web.pageable` defaults
- Flyway: `V4__catalog.sql`, `V5__inventory.sql`
- Tests: 67 passing — use-case unit tests (Mockito), `Inventory` domain invariants, and a full
  Testcontainers integration test (admin creates brand/category/product → async inventory seeding →
  browse/search/paginate → set stock → reserve → release; over-reserve 409; non-admin 403),
  Modulith verification

### Phase 3 — Cart & Coupon

- `cart` module
  - Aggregates: `Cart` (carts, one per customer) with `CartItem` children (cart_items)
  - Use cases: get-or-create cart, add item, update quantity, remove item
  - Each item snapshots the catalog product name + unit price at add time; availability is checked
    against inventory on add/update (over-cart → HTTP 409, inactive/missing product → 409/404)
  - Authenticated customer API: `GET /api/v1/cart`, `POST /api/v1/cart/items`,
    `PUT/DELETE /api/v1/cart/items/{itemId}` (customer resolved from the JWT principal)
  - Reads catalog/inventory via their new `spi` named interfaces (`CatalogQuery`, `InventoryQuery`)
  - Consumes catalog `ProductUpdatedEvent` (`@ApplicationModuleListener`) to refresh price/name
    snapshots; `StockUpdatedEvent` auto-trim deferred
  - MapStruct mapper (`CartMapper`)
- `coupon` module (dependency-free)
  - Aggregates: `Coupon` (coupons), `CouponUsage` (coupon_usages)
  - Use cases: create coupon (admin), validate coupon, apply coupon — validation/discount operate
    on an order amount supplied by the caller (percentage with optional cap, or fixed amount;
    validity window, minimum order, usage-limit rules enforced in the domain)
  - APIs: `POST /api/v1/admin/coupons` (`ROLE_ADMIN`), `POST /api/v1/coupons/validate`,
    `POST /api/v1/coupons/apply`
  - Events published (named interface): `CouponAppliedEvent`, `CouponExpiredEvent` (lazy expiry on
    validate/apply); per-order usage recording deferred to the order phase (delivered in Phase 4 via
    the `coupon.spi.CouponRedemption` command — coupon does not consume `OrderCreatedEvent`)
  - MapStruct mapper (`CouponMapper`)
- `catalog` / `inventory`: added read-only `spi` named interfaces (`CatalogQuery` + `ProductView`,
  `InventoryQuery`) so `cart` can read product/stock data without crossing module internals
- Flyway: `V6__cart.sql`, `V7__coupon.sql`
- Tests: cart & coupon domain invariants, use-case unit tests (Mockito), and a full Testcontainers
  integration test (create/stock product → add to cart with price snapshot → update qty →
  over-stock 409 → remove; admin creates coupon → validate → apply; non-admin 403),
  Modulith verification
- Note: cart checkout was deferred to the order phase. Delivered in Phase 4 **without** a
  `CheckoutCartUseCase` / `CartCheckedOutEvent` — the order module reads the cart via `cart.spi`
  (`CartQuery`) at checkout and clears it via `cart.spi.CartMaintenance` (avoids a module cycle)

### Phase 4 — Order

- `order` module
  - Aggregates: `Order` (orders) with `OrderItem` (order_items) and `OrderAddress` (order_addresses)
    children; `OrderStatus` state machine (`PENDING`→`PAID`/`PROCESSING`→`SHIPPED`→`DELIVERED`, plus
    `CANCELLED`/`REFUNDED`)
  - Use cases: place order (from a cart snapshot, optional coupon, `Idempotency-Key` dedupe),
    cancel order, get order, list orders (history), admin update-status
  - Customer API: `POST /api/v1/orders`, `GET /api/v1/orders` (paginated, `?status=`),
    `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/cancel` (own orders only)
  - Admin API (`ROLE_ADMIN`): `GET /api/v1/admin/orders`, `GET /api/v1/admin/orders/{id}`,
    `PUT /api/v1/admin/orders/{id}/status` (guarded transitions)
  - MapStruct mapper (`OrderMapper`); each line snapshots product name + unit price; the shipping
    address is snapshotted from the user module
  - Events published (named interface): `OrderCreatedEvent`, `OrderCancelledEvent`,
    `OrderCompletedEvent` (on delivery)
  - **Orchestration model (acyclic boundaries):** order reads cart/user/coupon and pre-checks
    inventory via their `spi` named interfaces, then orchestrates the modules it depends on via new
    `spi` commands. Because `cart -> inventory` already exists, no module that order (transitively)
    reads may depend back on order; so side effects are driven by order's own
    `@ApplicationModuleListener`s (post-commit, each in its own transaction):
    reserve/release stock (`inventory.spi.StockReservations`), clear the cart
    (`cart.spi.CartMaintenance`), record coupon usage linked to the order (`coupon.spi.CouponRedemption`)
  - New cross-module `spi`: `cart` (`CartQuery`/`CartView` + `CartMaintenance`), `user`
    (`UserQuery`/`AddressView`, resolving the customer from the auth user id), `coupon`
    (`CouponQuery`/`CouponQuote` read + `CouponRedemption` command), `inventory`
    (`StockReservations` reserve/release-by-reference)
- Flyway: `V8__order.sql` (orders, order_items, order_addresses; unique `(customer_id,
  idempotency_key)`)
- Tests: 148 passing — `Order` domain (totals, state machine), use-case unit tests (Mockito),
  `spi` service + order fulfilment-handler unit tests, and a full Testcontainers integration test
  (register → address → stock product → cart → place order → async cart-clear/stock-reserve →
  details/history → ownership 404 → cancel → async stock-release → coupon order → admin status
  lifecycle → non-admin 403 → idempotent replay), Modulith verification

### Phase 5 — Payment & Shipment

- `payment` module
  - Aggregates: `Payment` (payments, one per order) with `PaymentTransaction` children
    (payment_transactions, append-only gateway-interaction ledger); `PaymentStatus`
    (`PENDING`→`SUCCESS`/`FAILED`, `FAILED`→`PENDING` retry, `SUCCESS`→`REFUNDED`) and `PaymentMethod`
    (`CARD`/`PAYPAL`/`BANK_TRANSFER`)
  - Use cases: create a PENDING intent on `OrderCreatedEvent`; process a charge through a pluggable
    `PaymentGateway` (`SimulatedPaymentGateway` approves by default), with `Idempotency-Key` dedupe and
    failed-payment retry; get (status) and list (history); admin refund
  - Customer API: `POST /api/v1/payments`, `GET /api/v1/payments` (paginated, `?orderId=`),
    `GET /api/v1/payments/{id}`; Admin API (`ROLE_ADMIN`): `GET /api/v1/admin/payments/{id}`,
    `POST /api/v1/admin/payments/{id}/refund`
  - Events published (named interface): `PaymentCompletedEvent`, `PaymentFailedEvent`,
    `PaymentRefundedEvent`; MapStruct `PaymentMapper`
- `shipment` module
  - Aggregates: `Shipment` (shipments, one per order) with `TrackingRecord` children (tracking_records,
    append-only history) and an embedded `DeliveryAddress` snapshot; `ShipmentStatus`
    (`CREATED`→`PICKED_UP`→`IN_TRANSIT`→`OUT_FOR_DELIVERY`→`DELIVERED`)
  - Use cases: auto-create a shipment on `PaymentCompletedEvent` (snapshotting the address via the
    order `spi`); admin manual create (idempotent per order), track (owner/admin), advance status,
    confirm delivery
  - Customer API: `GET /api/v1/shipments/{id}`; Admin API (`ROLE_ADMIN`): `POST /api/v1/admin/shipments`,
    `GET /api/v1/admin/shipments/{id}`, `PUT /api/v1/admin/shipments/{id}/status`,
    `POST /api/v1/admin/shipments/{id}/deliver`
  - Events published (named interface): `ShipmentCreatedEvent`, `ShipmentDeliveredEvent`; MapStruct
    `ShipmentMapper`
- `order` module: new `order.spi` named interface — `OrderQuery`/`OrderView` (read an order) and
  `OrderLifecycle` ("ensure at least" `markPaid`/`markProcessing`/`markDelivered`, walking the happy
  path). `UpdateOrderStatusUseCase` refactored to share an `OrderStatusNotifier`
  - **Orchestration model (acyclic boundaries):** payment/shipment depend on order (never the
    reverse). Payment marks the order `PAID` and shipment advances it to `PROCESSING`/`DELIVERED` by
    *pushing* status through the order `spi` from post-commit `@ApplicationModuleListener`s — order
    does not consume payment/shipment events (that would cycle). The lifecycle calls are idempotent
    and order-tolerant, so the async events that drive them can arrive in any order
- Flyway: `V9__payment.sql`, `V10__shipment.sql`
- Tests: payment & shipment domain state machines, use-case unit tests (Mockito), `spi` service +
  event-handler unit tests, and a full Testcontainers integration test (register → cart → place order →
  async payment intent → pay → async order PAID → async shipment auto-create → track → advance status →
  deliver → async order DELIVERED; payment idempotency; admin refund; ownership/admin-only auth),
  Modulith verification

## In Progress

None

## Next

- Phase 6: Notification (`notification` module) — event-driven (consumes user/order/payment/shipment
  events); publishes `NotificationSentEvent`

## Current Branch

main