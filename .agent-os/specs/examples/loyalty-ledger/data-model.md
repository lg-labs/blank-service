---
kind: example-data-model
feature: 001-loyalty-ledger
version: 0.1.0
status: example
description: Aggregates, events, outbox payloads, REST DTOs, Avro schemas, and JPA tables for loyalty-ledger.
---

# Data model — `loyalty-ledger` (example)

## Bounded context

`loyalty` — owns customer point balances and ledger history. Consumes
order signals; emits credit notifications. Does **not** own customer
identity (customerId is opaque) or order data (orderId is opaque).

## Aggregates & entities (domain-core)

### `Balance` (aggregate root)
| Field | Type | Notes |
|-------|------|-------|
| `customerId` | `CustomerId` (BaseId) | identity |
| `points` | `int` | invariant: `points >= 0` |
| `version` | `int` | optimistic locking (RULE-008) |

Behavior: `credit(int amount)` (must be > 0), `debit(int amount)` (out of
scope v1 but invariant declared).

### `LedgerEntry` (entity inside Balance)
| Field | Type | Notes |
|-------|------|-------|
| `entryId` | `LedgerEntryId` | identity |
| `customerId` | `CustomerId` | FK to Balance |
| `delta` | `int` | signed amount; `+N` credit, `-N` debit |
| `reason` | `LedgerReason` enum | `ORDER_PAID`, … |
| `causationId` | `String` | source signal id (e.g. orderId) |
| `occurredAt` | `Instant` | event time, monotonic per customer |

## Domain events (domain-core)

| Event | Payload | When raised |
|-------|---------|-------------|
| `PointsCredited` | `customerId, delta, balanceAfter, causationId, occurredAt` | After `Balance.credit` succeeds |

## Outbox payloads (application-service)

`PointsCreditedEventPayload` (Lombok record). Exact shape that goes into
the `outbox.payload` jsonb column. Distinct from the domain event
(RULE-008): serialization concerns + saga metadata live here.

| Field | Type |
|-------|------|
| `customerId` | `String` (UUID text) |
| `delta` | `int` |
| `balanceAfter` | `int` |
| `causationId` | `String` |
| `occurredAt` | `Instant` |

## REST DTOs (api)

`BalanceResponse` (record): `customerId, points`.
`LedgerEntryResponse` (record): `entryId, delta, reason, occurredAt`.
`LedgerPageResponse` (record): `entries, nextCursor`.

All produced as `application/vnd.api.v1+json` (RULE-006).

## Avro schemas (message-model)

### `points-credited.avsc` (produced)
```json
{
  "type": "record",
  "name": "PointsCreditedAvroModel",
  "namespace": "com.example.loyalty.message.avro",
  "fields": [
    { "name": "customerId",   "type": "string" },
    { "name": "delta",        "type": "int"    },
    { "name": "balanceAfter", "type": "int"    },
    { "name": "causationId",  "type": "string" },
    { "name": "occurredAt",   "type": { "type": "long", "logicalType": "timestamp-millis" } }
  ]
}
```

Compatibility mode: `BACKWARD` (RULE-007).

### `OrderPaidAvroModel` (consumed)

Imported from `order-service-message-model` (ADR-002). Not redeclared.

## JPA tables (data-access)

Schema: `"loyalty"` (quoted).

### `balance`
| Column | Type | Constraints |
|--------|------|-------------|
| `customer_id` | `uuid` | PK |
| `points` | `int` | `not null check (points >= 0)` |
| `version` | `int` | `not null` |

### `ledger_entry`
| Column | Type | Constraints |
|--------|------|-------------|
| `entry_id` | `uuid` | PK |
| `customer_id` | `uuid` | FK → `balance.customer_id` |
| `delta` | `int` | `not null` |
| `reason` | `text` | `not null` (Postgres native enum + JPA `EnumType.STRING`) |
| `causation_id` | `text` | `not null` |
| `occurred_at` | `timestamptz` | `not null` |

Index: `ledger_entry (customer_id, occurred_at desc)` for paginated reads.

### `outbox` (per RULE-008 standard shape)
| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `uuid` | PK |
| `saga_id` | `uuid` | nullable (no saga in v1) |
| `type` | `text` | e.g. `points-credited` |
| `payload` | `jsonb` | (declared as `String` in JPA, NO `@JdbcTypeCode`) |
| `outbox_status` | `text` | `STARTED|COMPLETED|FAILED` (Postgres enum) |
| `version` | `int` | optimistic locking |
| `created_at` | `timestamptz` | not null |

Indexes: `(type, outbox_status)` range; `(type, causation_id)` unique
(double-credit guard).

## Idempotency strategy (REQ-004)

Before crediting, the use case helper queries the outbox by
`(type='points-credited', causation_id=orderId)`. If a row exists, the
listener returns NO-OP (RULE-010). Otherwise it credits + writes the
outbox row in the same `@Transactional` boundary.
