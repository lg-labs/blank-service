---
description: Add an end-to-end SagaStep to a service — publisher, listener, outbox, scheduler, and idempotency guards. Wires the step into an existing saga or starts a new one.
argument-hint: <service-name> <step-name> [--orchestrator <existing-saga>]
allowed-tools: bash, read, write, edit, glob, grep
---

# /add-saga

You are adding a `SagaStep<T>` to a lg5-spring microservice with all the
plumbing required by the framework: outbox table, outbox helper, scheduler,
Kafka publisher, Kafka listener, and the idempotency guards. Follow
RULE-008, RULE-009, RULE-010, RULE-011 strictly.

## Inputs

- `<service-name>` — the existing service to add the step to (must already
  have been scaffolded by `/scaffold-service`).
- `<step-name>` — the step name in CamelCase (e.g. `Payment`, `RestaurantApproval`).
- `--orchestrator <existing-saga>` — optional. If provided, this step is added
  to the existing saga. If omitted, a new saga orchestrator is created.

If the user provided fewer arguments than required, ask them for the missing
ones BEFORE making any file changes.

## Pre-flight checks

1. The target service must exist with the canonical 8-module shape (RULE-004).
2. The `<service-name>-message-model` module must already have at least one
   `.avsc` schema file (we will add a request and response schema for this
   step). If not, the service is missing message infrastructure — set it up
   first via `/add-kafka-listener` or by hand.
3. Confirm with the user the **payload shape** of the step:
   - request fields (e.g. `sagaId`, `customerId`, `amount`)
   - response fields (e.g. `sagaId`, `paymentId`, `status`)

## Steps

For each step, summarize before/after as you did in `/scaffold-service`.

1. **Avro schemas** in `<service-name>-message-model/src/main/resources/avro/`:
   - `<step-name>RequestAvroModel.avsc`
   - `<step-name>ResponseAvroModel.avsc`
   Schemas must include `sagaId` (string, logicalType uuid) as the Kafka key
   field (RULE-007). Run `make run-avro-model` to regenerate sources.

2. **Outbox JPA entity** in `<service-name>-application-service/.../outbox/model/`:
   - `<step-name>OutboxEntity.java` with the required fields per RULE-008
     (`@Id`, `@Version`, `OutboxStatus`, `SagaStatus`, `sagaId`, `type`,
     `payload`, timestamps).
   - Pair it with a Lombok `<step-name>EventPayload` class (different from
     the domain event — see RULE-008 references and the
     `food-ordering-system` skill).

3. **Outbox DDL** in `<service-name>-data-access/src/main/resources/db/migration/`
   (Flyway migration file). Create:
   - the `<step-name|lower>_outbox` table with `payload jsonb` and native
     PG enums for `outbox_status` and `<step-name|lower>_status`;
   - range index on `(type, outbox_status, saga_status)`;
   - unique index on `(type, saga_id, saga_status|<step-name>_status)`.
   See RULE-008 for the JPA-vs-DDL asymmetry.

4. **Outbox helper** in `<service-name>-application-service/.../outbox/`:
   - `<step-name>OutboxHelper.java` with `getXxxByOutboxStatusAndSagaStatus`
     and `getXxxBySagaIdAndSagaStatus` query methods (Optional<List<...>>
     return types so callers can short-circuit on empty — RULE-009).

5. **Outbox scheduler** in `<service-name>-application-service/.../outbox/scheduler/`:
   - `<step-name>OutboxScheduler.java` per RULE-011 (implements
     `OutboxScheduler`, gated by `@ConditionalOnProperty("scheduling.enabled")`,
     `@Scheduled(fixedDelayString = "${<service-name>-service.outbox-scheduler-fixed-rate}")`).

6. **Kafka publisher** in `<service-name>-message-core/.../publisher/`:
   - `<step-name>RequestKafkaPublisher.java` generic over
     `KafkaProducerHelper<String, <step-name>RequestAvroModel>` (RULE-007).

7. **Kafka listener** in `<service-name>-message-core/.../listener/`:
   - `<step-name>ResponseKafkaListener.java` per RULE-010 (batch listener,
     swallows `OptimisticLockingFailureException` + not-found exceptions
     as NO-OP).

8. **SagaStep implementation** in `<service-name>-application-service/.../saga/`:
   - `<step-name>SagaStep.java` per RULE-009 (`@Component`, `process` and
     `rollback` `@Transactional`, idempotency guard via outbox query).

9. **application.yaml** updates in `<service-name>-container/src/main/resources/`
   under the `<service-name>-service.*` prefix (RULE-014):
   - topic names: `<step-name|lower>-request-topic-name`, `<step-name|lower>-response-topic-name`
   - `outbox-scheduler-fixed-rate` (if not already set)

10. **IT under `<service-name>-container/src/test/java/.../<step-name>SagaIT.java`**
    extending `Lg5TestBoot` (RULE-012), driven by `@Sql("/sql/<step-name>-it-data.sql")`,
    asserting the happy path and the rollback path. SQL fixtures go in
    `<service-name>-container/src/test/resources/sql/`.

11. **Sanity build**: `make install-skip-test` then `make run-acceptance-test`
    if the user has Docker available.

12. **Final report**:
    - all files created (paths + role)
    - the `(sagaId, status)` transitions encoded
    - the next step the user needs to do (typically: implement the
      domain logic inside the helper's `persist*` method — that is
      service-specific and out of scope for this command).

## Anti-patterns to avoid

- DO NOT publish to Kafka from inside the business `@Transactional` (RULE-008).
- DO NOT skip `@Version` on the outbox entity (RULE-008).
- DO NOT skip the outbox-by-`(sagaId, status)` pre-check in `process`/`rollback`
  (RULE-009).
- DO NOT rethrow `OptimisticLockingFailureException` from the listener (RULE-010).
- DO NOT hardcode the scheduler cadence — always read from
  `<service-name>-service.outbox-scheduler-fixed-rate` (RULE-011, RULE-014).
- DO NOT publish JSON over Kafka (RULE-007).

## References

- Skill: `lg5-saga` (full saga implementation walkthrough).
- Skill: `lg5-outbox` (outbox helper + scheduler patterns).
- Skill: `lg5-kafka-avro` (publisher + listener with Avro).
- Reference: `food-ordering-system/order-service/.../saga/OrderPaymentSaga.java`.
- Rules: RULE-007, RULE-008, RULE-009, RULE-010, RULE-011, RULE-014.
