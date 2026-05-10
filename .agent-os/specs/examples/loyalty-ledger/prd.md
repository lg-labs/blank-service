---
kind: example-prd
feature: 001-loyalty-ledger
version: 0.1.0
status: example
description: PRD section of the illustrative loyalty-ledger spec example.
---

# PRD — `loyalty-ledger` (example)

> **This is an example.** It illustrates the shape of a PRD authored with
> `specs/templates/prd-template.md` and `/sdd-specify`. Do not implement
> as-is.

## 1. Summary

The `loyalty-ledger` service stores customer loyalty point balances and
emits append-only ledger entries on every credit/debit. It exposes a small
REST API for read-only balance queries and consumes `OrderPaidEvent` from
the existing `order-service` to credit points automatically.

## 2. Problem

Today, loyalty points are calculated on-the-fly in the order-service when
an order is paid, and there is no source of truth for "current balance"
or "ledger history". Customer Support cannot answer "why did my balance
drop?" without a manual SQL trip through the orders DB.

## 3. Users

- **Customer (read-only)** — wants to check balance from the mobile app.
- **Customer Support agent** — wants to view a per-customer ledger.
- **order-service (system)** — needs a fire-and-forget way to credit points.

## 4. Success metrics

| Metric                              | Baseline | Target  | Window |
|-------------------------------------|---------:|--------:|--------|
| Balance read p95 latency            | N/A      | <50ms   | 30d    |
| % of OrderPaid events credited      | N/A      | >99.5%  | 30d    |
| Support tickets "wrong balance"     | 12/wk    | <2/wk   | 60d    |

## 5. Requirements (in scope)

| ID | Requirement | Acceptance |
|----|-------------|------------|
| REQ-001 | Read current balance for a customer | `GET balance` returns 200 with the value, or 404 if unknown |
| REQ-002 | Read paginated ledger history for a customer | `GET ledger?from=&to=` returns 200 with paginated entries |
| REQ-003 | Credit points automatically when an order is paid | A paid-order signal triggers a credit equal to the configured rate |
| REQ-004 | Idempotent re-delivery of the same paid-order signal | A re-delivered signal does not double-credit |
| REQ-005 | Notify downstream when a credit happens | A credit emits a `points-credited` business event reliably |

## 6. Out of scope

- Point **debit** flow (purchases with points) — _(reason: needs a saga
  with order-service; v2)_.
- UI for Customer Support — _(reason: backend only this iteration)_.
- Multi-currency / multi-tier loyalty programs — _(reason: single
  flat-rate rule for v1)_.

## 7. Acceptance criteria (feature-level)

- [ ] `make install-skip-test` succeeds.
- [ ] ATDD: "Crediting points on OrderPaid" scenario green.
- [ ] ATDD: "Double-delivered OrderPaid is idempotent" scenario green.
- [ ] ATDD: "Balance not found returns 404" scenario green.
- [ ] No `must` violations reported by `lg5-code-reviewer` subagent.
- [ ] `points-credited` event is observed on the downstream channel.

## 8. Open questions

| Question | Decider | Due |
|---------|---------|-----|
| Points-per-dollar rate (v1 hard-coded? config? table?) | Product | <date> |
| Retention policy for the ledger history | Compliance | <date> |
| Partitioning key for the credit signal (customerId vs orderId)? | Platform | <date> |

## Definition of Done (PRD)

- [ ] Every requirement has a stable ID (REQ-NNN).
- [ ] No technology mentioned (no Spring, Kafka, Postgres, REST, …).
- [ ] Every requirement has at least one acceptance criterion.
- [ ] Pending clarifications marked with `[NEEDS CLARIFICATION: …]`.
- [ ] Out-of-scope items explicitly listed with reason.
- [ ] Stakeholder/owner identified (in the open questions table).
