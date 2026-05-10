---
kind: example-readme
feature: 001-loyalty-ledger
version: 0.1.0
status: example
description: How this example was built and how to read it.
---

# Example — `001-loyalty-ledger`

This folder is an **illustrative, end-to-end SDD spec** for a hypothetical
microservice (`loyalty-ledger`). It is included only to show the shape
that a real service's `docs/specs/<NNN-slug>/` folder takes after running
the SDD workflow described in [`../../README.md`](../../README.md).

> **Do not implement this service as-is.** It is a teaching artifact.

## What's here

| File | Produced by | Purpose |
|------|-------------|---------|
| [`prd.md`](prd.md) | `/sdd-specify` | Functional spec (REQ-NNN with AC); no technology |
| [`adr/ADR-001-*.md`](adr/) | `/sdd-plan` | First architectural decision (outbox vs saga) |
| [`adr/ADR-002-*.md`](adr/) | `/sdd-plan` | Second architectural decision (Avro model reuse) |
| [`plan.md`](plan.md) | `/sdd-plan` | Module map, ADR index, dep graph, risks |
| [`data-model.md`](data-model.md) | `/sdd-plan` | Aggregates, events, outbox, JPA, Avro |
| [`tasks.md`](tasks.md) | `/sdd-tasks` | 9 atomic TASK-NNN with Given/When/Then AC |

## Reading order

1. **`prd.md`** — what & why (purely functional).
2. **`adr/ADR-001`** then **`adr/ADR-002`** — why we chose this shape.
3. **`plan.md`** — module map, dep graph, risks.
4. **`data-model.md`** — concrete shapes (entities, schemas, tables).
5. **`tasks.md`** — what gets built, in what order.

## What this example demonstrates

- A PRD that is **technology-free** (no Spring/Kafka/Postgres mentioned).
- ADRs that explicitly state their **constitutional impact** by RULE-ID.
- A `plan.md` whose module map matches **RULE-004** byte-for-byte.
- A `data-model.md` that separates **domain events** from **outbox payloads**
  (RULE-008 nuance).
- A `tasks.md` whose **dependency graph** is acyclic and whose first/last
  TASKs match the bundle's Definition of Done.

## What this example does NOT demonstrate

- A saga step (`SagaStep<T>`) — the example deliberately opts out (ADR-001).
  See `skills/lg5-saga` and `commands/add-saga.md` for the saga shape.
- Compensation paths.
- Multi-event listeners or batching nuances.
