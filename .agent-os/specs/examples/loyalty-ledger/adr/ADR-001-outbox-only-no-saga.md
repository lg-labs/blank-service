---
kind: example-adr
feature: 001-loyalty-ledger
version: 0.1.0
adr-id: ADR-001
status: accepted
date: 2026-05-09
description: Outbox-only emission, no saga participation in v1.
---

# ADR-001 — Outbox-only emission, no saga participation in v1

## Status

Accepted — 2026-05-09

## Context

The credit operation triggered by `OrderPaidEvent` has no compensating
write to issue if the downstream `points-credited` event fails to publish:
the ledger entry is the source of truth, and a missing publish only delays
the marketing-side notifications, which is acceptable.

## Decision

Use the Transactional Outbox pattern (RULE-008) to persist
`points-credited` events alongside the ledger entry, but **do not** make
the credit a `SagaStep<T>` (no orchestrator, no rollback path).

## Constitutional impact

- **RULE-008** (outbox mandatory) — confirmed (we use the outbox).
- **RULE-009** (saga step idempotent) — explicitly opts out (no SagaStep);
  idempotency is enforced at the listener level via the outbox guard pattern
  (query existing outbox row by `(orderId)` before crediting).
- **RULE-011** (outbox scheduler shape) — confirmed (standard scheduler).

No constitutional violations.

## Alternatives considered

- **Saga with order-service compensation** — pros: end-to-end consistency
  with order state. Cons: order-service does not need to know about
  loyalty; introduces unnecessary cross-service coupling. Rejected because
  no business need for compensation in v1.
- **Direct broker publish from the @Transactional credit method** — pros:
  fewer moving parts. Cons: violates RULE-008 (atomicity loss). Rejected:
  rule-blocked.

## Consequences

- **Positive:** simpler implementation; ~50% fewer files than a full saga.
- **Negative:** if compensation becomes a requirement later, refactor to
  `SagaStep<T>` is non-trivial.
- **Neutral:** still uses `OutboxStatus` and the standard scheduler so the
  migration path is a localized change.
