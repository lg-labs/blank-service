---
description: Add a Transactional Outbox (JPA entity + DDL migration + helper + scheduler) for a single event type, without the full saga plumbing.
argument-hint: <service-name> <event-name>
allowed-tools: bash, read, write, edit, glob, grep
---

# /add-outbox

You are adding a Transactional Outbox to a lg5-spring microservice for a
single event type. Use this when the service needs to publish a domain event
that crosses a service boundary but is NOT (yet) part of a multi-step saga.
For full saga plumbing, use `/add-saga` instead.

Follow RULE-008 and RULE-011 strictly.

## Inputs

- `<service-name>` — the existing service (must have the canonical 8-module shape).
- `<event-name>` — the domain event in CamelCase (e.g. `OrderPlaced`,
  `RefundIssued`). The outbox table will be `<event-name|snake_lower>_outbox`.

If the user provided fewer arguments than required, ask BEFORE writing any files.

## Pre-flight checks

1. Confirm the service has been scaffolded (8 modules per RULE-004).
2. Confirm the user wants OUT-only outbox (no saga). If they want a saga,
   redirect them to `/add-saga`.
3. Confirm the payload shape: which fields go in the JSON `payload` column?
   The agent should sketch the `<event-name>EventPayload` Lombok class with
   the user before writing files.

## Steps

1. **Outbox JPA entity** at
   `<service-name>-application-service/.../outbox/model/<event-name>OutboxEntity.java`:
   - `@Id UUID id`
   - `@Version int version` — REQUIRED (RULE-008)
   - `@Enumerated(EnumType.STRING) OutboxStatus outboxStatus` — REQUIRED
   - `UUID sagaId` (nullable if non-saga; conventional even for non-saga so
     the schema is uniform)
   - `String type` — the event name string
   - `String payload` — JSON serialized `<event-name>EventPayload`
   - `ZonedDateTime createdAt`, `processedAt`
   Mark with `@Table(name = "<event-name|snake_lower>_outbox", schema = "\"<service-name>\"")`.

2. **Outbox payload class** at the same package:
   `<event-name>EventPayload.java` — Lombok `@Builder @Getter @AllArgsConstructor`
   record-style class with the fields the user agreed on. This is DIFFERENT
   from the domain event in `<service-name>-domain-core` (RULE-008 anti-pattern
   discussion).

3. **DDL Flyway migration** in
   `<service-name>-data-access/src/main/resources/db/migration/V<n>__create_<event-name|snake_lower>_outbox.sql`:
   ```sql
   CREATE SCHEMA IF NOT EXISTS "<service-name>";
   CREATE TYPE "<service-name>".outbox_status AS ENUM ('STARTED', 'COMPLETED', 'FAILED');
   CREATE TABLE "<service-name>".<event-name|snake_lower>_outbox (
       id            UUID PRIMARY KEY,
       version       INTEGER NOT NULL,
       outbox_status "<service-name>".outbox_status NOT NULL,
       saga_id       UUID,
       type          VARCHAR(255) NOT NULL,
       payload       JSONB NOT NULL,
       created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
       processed_at  TIMESTAMP WITH TIME ZONE
   );
   CREATE INDEX <event-name|snake_lower>_outbox_status_idx
       ON "<service-name>".<event-name|snake_lower>_outbox (type, outbox_status);
   ```
   Note: `outbox_status` is a NATIVE enum at DDL level but `EnumType.STRING`
   in JPA — the cast is implicit (RULE-008).

4. **Outbox helper** at
   `<service-name>-application-service/.../outbox/<event-name>OutboxHelper.java`:
   - `Optional<List<<event-name>OutboxMessage>> getByOutboxStatus(OutboxStatus s)`
     — used by the scheduler.
   - `<event-name>OutboxMessage save(<event-name>OutboxMessage m)` — wraps the JPA save.
   - `void persist<event-name>(<businessParams>)` annotated `@Transactional` —
     called by the application service to write the business row + outbox row
     in one transaction.

5. **Outbox scheduler** at
   `<service-name>-application-service/.../outbox/scheduler/<event-name>OutboxScheduler.java`
   per RULE-011:
   ```java
   @Component
   @RequiredArgsConstructor
   @ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
   @Slf4j
   public class <event-name>OutboxScheduler implements OutboxScheduler {
       @Override
       @Transactional
       @Scheduled(fixedDelayString = "${<service-name>-service.outbox-scheduler-fixed-rate}")
       public void processOutboxMessage() { /* … */ }
   }
   ```

6. **Kafka publisher** at
   `<service-name>-message-core/.../publisher/<event-name>KafkaPublisher.java` —
   thin wrapper over `KafkaProducerHelper<String, <event-name>AvroModel>`. The
   Avro schema must already exist at
   `<service-name>-message-model/src/main/resources/avro/<event-name>AvroModel.avsc`
   (RULE-007). If not, ask the user to define it first or run `/add-kafka-listener`.

7. **application.yaml** updates under `<service-name>-service.*` (RULE-014):
   - `<event-name|kebab>-topic-name: <event-name|kebab>` (or whatever the user wants)
   - `outbox-scheduler-fixed-rate: 1000` (if not already set)

8. **application-test.yaml**: ensure `scheduling.enabled: false` (RULE-011)
   so IT/ATDD don't see spurious scheduler ticks.

9. **IT** at `<service-name>-container/src/test/java/.../<event-name>OutboxIT.java`
   extending `Lg5TestBoot[PortNone]` (RULE-012), asserting:
   - business save + outbox save happen atomically;
   - scheduler picks up `STARTED` rows (when explicitly enabled in the test);
   - `OptimisticLockingFailureException` is raised on concurrent update of
     the same row.

10. **Sanity build**: `make install-skip-test`. Then `make docker-up` +
    `make run-apps` if the user wants to verify end-to-end against local
    Postgres + Kafka.

11. **Final report**:
    - files created
    - the JPA-vs-DDL asymmetry one-line reminder (`payload` is `String` in
      JPA but `jsonb` in DDL; do NOT add `@JdbcTypeCode(SqlTypes.JSON)`).
    - reminder to call `outboxHelper.persist<event-name>(...)` from the
      application service inside the business `@Transactional`.

## Anti-patterns to avoid

- DO NOT skip `@Version` (RULE-008).
- DO NOT add `@JdbcTypeCode(SqlTypes.JSON)` to the `payload` field — the
  framework convention is plain `String` in JPA, native `jsonb` at DDL,
  with the implicit cast doing the bridge (RULE-008).
- DO NOT publish to Kafka from inside the business `@Transactional` (RULE-008).
- DO NOT use `@Scheduled(fixedRate = ...)` — must be `fixedDelayString` from
  property (RULE-011).
- DO NOT skip `@ConditionalOnProperty("scheduling.enabled")` (RULE-011, RULE-013).

## References

- Skill: `lg5-outbox` (full DDL + entity + helper + scheduler walkthrough).
- Skill: `food-ordering-system` (real-world examples of `OrderOutboxEntity`,
  `PaymentOutboxEntity`, `RestaurantApprovalOutboxEntity`).
- Rules: RULE-007, RULE-008, RULE-011, RULE-014.
