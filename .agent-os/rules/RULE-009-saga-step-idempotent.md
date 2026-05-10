---
id: RULE-009
slug: saga-step-idempotent
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: saga
tags: [saga, saga-step, idempotency, transactional, rollback]
description: Saga steps implement `com.lg5.spring.saga.SagaStep<T>` as `@Component`. `process` and `rollback` are `@Transactional` and idempotent — query the outbox by (sagaId, expected SagaStatus) and return early if not found.
---

# RULE-009 — Saga step shape and idempotency

## Statement

A saga step is a Spring bean that implements:

```java
package com.lg5.spring.saga;

public interface SagaStep<T> {
    void process(T data);
    void rollback(T data);
}
```

with the following hard requirements:

1. The bean is annotated `@Component` (or `@Service`) and constructor-injected
   via Lombok `@RequiredArgsConstructor`. **Never** invent a custom
   annotation like `@SagaStep` (see RULE-005).
2. Both `process` and `rollback` carry `@Transactional`. They are the
   transactional boundary for that step — the helper they delegate to is also
   `@Transactional` and joins the same physical transaction.
3. Both methods are **idempotent**: the first thing they do is query the
   relevant outbox by `(sagaId, expected SagaStatus)`. If the row is not
   found, the method returns early — the step has already been processed
   (Kafka redelivery, scheduler double-tick, retry).
4. Both methods are responsible for advancing the outbox row from one
   `SagaStatus` to the next, atomically with the business state change.

## Rationale

Sagas in lg5-spring run in a **choreography over Kafka** with the outbox as
the durable coordination point. Because Kafka delivery is at-least-once and
the outbox scheduler may tick more than once before the broker acks, every
step is guaranteed to be invoked multiple times for the same
`(sagaId, status)` tuple. If `process`/`rollback` are not idempotent, you get:

- Duplicate side effects (e.g. charging the customer twice).
- `OptimisticLockingFailureException` on the outbox row when two threads race
  to the same update — uncaught, the Kafka listener rethrows and the broker
  redelivers, creating an infinite loop.

The outbox-by-`(sagaId, status)` pre-check is the canonical guard: if the
expected row is gone (already advanced), there is nothing left to do.

## Example — correct

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaStep implements SagaStep<PaymentResponse> {

    private final PaymentResponseHelper helper;

    @Override
    @Transactional
    public void process(final PaymentResponse response) {
        log.info("Saga step process for sagaId={}", response.getSagaId());
        helper.persistPaymentResponse(response, SagaStatus.STARTED);
        // helper internally:
        //  1. SELECTs outbox by (sagaId, STARTED). If absent → return (idempotent).
        //  2. Loads aggregate, applies domain event.
        //  3. UPDATEs business row.
        //  4. UPDATEs outbox row sagaStatus → PROCESSING.
        //  5. INSERTs next-step outbox with sagaStatus=STARTED.
    }

    @Override
    @Transactional
    public void rollback(final PaymentResponse response) {
        log.info("Saga step rollback for sagaId={}", response.getSagaId());
        helper.persistPaymentResponseRollback(response);
        // mirror image: SELECT by (sagaId, COMPENSATING) → if absent return; otherwise rollback.
    }
}
```

## Anti-pattern

```java
// WRONG: no outbox pre-check, no @Transactional, custom annotation
@SagaParticipant
public class PaymentSagaStep {
    public void process(final PaymentResponse r) {
        paymentRepository.charge(r.getCustomerId(), r.getAmount());   // ❌ duplicates on retry
    }
}
```

```java
// WRONG: @Transactional but no idempotency guard — second invocation
// throws OptimisticLockingFailureException which propagates to Kafka.
@Override @Transactional
public void process(final PaymentResponse r) {
    final Payment payment = paymentRepository.findById(r.getPaymentId()).orElseThrow();
    payment.charge();
    paymentRepository.save(payment);
    // ❌ no SELECT outbox by (sagaId, STARTED) before doing the work.
}
```

## References

- Skill: `lg5-saga` (full saga implementation walkthrough including
  orchestrator + participant patterns + outbox status transitions).
- Reference: food-ordering-system `OrderPaymentSaga`, `OrderApprovalSaga`.
- Related rules: RULE-008 (outbox underpins idempotency), RULE-010
  (listener swallows the OptimisticLock when the guard misses), RULE-005
  (no `@SagaParticipant` invented annotation).
