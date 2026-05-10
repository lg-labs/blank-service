---
kind: example-adr
feature: 001-loyalty-ledger
version: 0.1.0
adr-id: ADR-002
status: accepted
date: 2026-05-09
description: Reuse order-service-message-model for the consumed Avro schema.
---

# ADR-002 — Reuse `order-service-message-model` for the consumed Avro schema

## Status

Accepted — 2026-05-09

## Context

`order-paid` events are produced by `order-service` from a schema declared
in `order-service-message-model`. We need to deserialize the same wire
shape on the consumer side.

## Decision

Add `order-service-message-model` as a Maven dependency in
`loyalty-ledger-message-core` and use the generated `OrderPaidAvroModel`
class directly.

## Constitutional impact

- **RULE-007** (kafka-avro-payloads) — confirmed; Avro is the contract,
  and a single source of truth eliminates schema drift.
- **RULE-014** (configuration prefixes) — irrelevant (no new config keys).

No constitutional violations.

## Alternatives considered

- **Re-declare the schema in `loyalty-ledger-message-model`** — pros: zero
  coupling to order-service's repo. Cons: schema drift risk; two sources
  of truth for the same wire shape. Rejected: violates the spirit of
  RULE-007.

## Consequences

- **Positive:** schema drift impossible; consumer breaks at compile-time
  if the producer changes the schema in a backwards-incompatible way.
- **Negative:** introduces a Maven dependency on `order-service-message-model`,
  which transitively pulls Confluent libraries — acceptable since we
  already need them.
