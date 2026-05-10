---
name: lg5-outbox
version: 0.1.0
lg5-spring-sha: d0d754a
last-validated: 2026-05-09
description: How to implement the Transactional Outbox pattern in lg5-spring — JPA outbox entity with optimistic locking, OutboxScheduler relay, status transitions. Load this skill when the user asks about outbox, dual-write problem, reliable event publishing, scheduled message relay, or needs to add an outbox to a service.
---

# lg5-spring — Transactional Outbox Pattern

> Framework abstractions: `com.lg5.spring.outbox.OutboxStatus` (enum) and `com.lg5.spring.outbox.OutboxScheduler` (interface) — both Kotlin in `lg5-spring-outbox`.
> Reference impl: `/tmp/lg5-study/food-ordering-system/order-service/order-domain/order-application-service/src/main/java/.../outbox/scheduler/payment/PaymentOutboxScheduler.java` and `.../order-data-access/.../outbox/payment/entity/PaymentOutboxEntity.java`.

## What the framework gives you

```kotlin
// /tmp/lg5-study/lg5-spring/lg5-spring-outbox/src/main/kotlin/com/lg5/spring/outbox/OutboxStatus.kt
enum class OutboxStatus { STARTED, COMPLETED, FAILED }

// /tmp/lg5-study/lg5-spring/lg5-spring-outbox/src/main/kotlin/com/lg5/spring/outbox/OutboxScheduler.kt
interface OutboxScheduler {
    fun processOutboxMessage()
}
```

Everything else (entity, repository, helper, scheduler logic, publisher binding) is implemented per-service.

## When to use it

Whenever a domain mutation must trigger an event that crosses a service boundary. The dual-write problem (DB commit + Kafka publish in one logical step) is solved by:

1. Same DB transaction writes both the aggregate change and an outbox row (`OutboxStatus.STARTED`).
2. A scheduler polls outbox rows in `STARTED` and publishes them to Kafka.
3. Kafka ack callback flips the row to `COMPLETED` (or `FAILED` on error).

## JPA entity shape (mandatory fields)

> **Important asymmetry**: the JPA mapping uses plain `String` for `payload` and `EnumType.STRING` for status columns. The **DDL** declares `payload jsonb` and the status columns as **native Postgres `ENUM` types** (e.g. `outbox_status`, `payment_status`, `approval_status`). Hibernate writes `String` and Postgres implicitly casts to `jsonb` / enum. Do not add `@JdbcTypeCode(SqlTypes.JSON)` — the food-ordering-system codebase never does, and adding it would break the implicit-cast path.

```java
// food-ordering-system/.../payment-data-access/.../outbox/entity/OrderOutboxEntity.java
@Entity
@Table(name = "order_outbox", schema = "payment")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class OrderOutboxEntity {

    @Id
    private UUID id;

    private UUID sagaId;

    @CreationTimestamp
    private ZonedDateTime createdAt;

    private ZonedDateTime processedAt;

    private String type;       // event type discriminator

    private String payload;    // ← plain String. DDL column is jsonb (implicit cast).

    @Enumerated(EnumType.STRING)
    private OutboxStatus outboxStatus;   // ← framework enum; DDL column is PG native enum

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus; // ← per-service aggregate status; DDL column is PG native enum

    @Version
    private int version;       // ← MANDATORY: optimistic locking
}
```

For order-service the schema name is a reserved word, so it must be quoted both in DDL and in the JPA annotation:

```java
@Table(name = "payment_outbox", schema = "\"order\"")
```

**Required columns**:
- `id` (UUID, PK)
- `sagaId` (UUID) — correlation across services
- `payload` — JSON-serialized event payload (`String` in JPA, `jsonb` in DDL)
- `outboxStatus` (`OutboxStatus`) — `EnumType.STRING` in JPA, native PG enum in DDL
- `sagaStatus` and/or aggregate status (per-service enum) — same mapping as above
- `version` (int, `@Version`) — **non-negotiable**, prevents lost updates between scheduler and saga step
- `createdAt`, `processedAt` for observability

## DDL (Postgres) — schema-per-service + native enums

food-ordering-system gives each service its own schema and defines the status columns as **native Postgres enum types**:

```sql
-- payment-service/payment-container/src/main/resources/init-schema.sql
DROP SCHEMA IF EXISTS payment CASCADE;
CREATE SCHEMA payment;

DROP TYPE IF EXISTS payment_status;
CREATE TYPE payment_status AS ENUM ('COMPLETED', 'CANCELLED', 'FAILED');

DROP TYPE IF EXISTS outbox_status;
CREATE TYPE outbox_status AS ENUM ('STARTED', 'COMPLETED', 'FAILED');

CREATE TABLE "payment".order_outbox (
    id             uuid                     NOT NULL,
    saga_id        uuid                     NOT NULL,
    created_at     timestamp with time zone NOT NULL,
    processed_at   timestamp with time zone,
    type           character varying        NOT NULL,
    payload        jsonb                    NOT NULL,
    outbox_status  outbox_status            NOT NULL,
    payment_status payment_status           NOT NULL,
    version        integer                  NOT NULL,
    CONSTRAINT order_outbox_pkey PRIMARY KEY (id)
);
CREATE INDEX "order_outbox_outboxStatus"
    ON "payment".order_outbox (outbox_status);
CREATE UNIQUE INDEX "order_outbox_sagaId_paymentStatus_outboxStatus"
    ON "payment".order_outbox (type, saga_id, payment_status, outbox_status);
```

For order-service (reserved word):

```sql
CREATE TABLE "order".payment_outbox (
    id              uuid PRIMARY KEY,
    saga_id         uuid NOT NULL,
    created_at      timestamp with time zone NOT NULL,
    processed_at    timestamp with time zone,
    type            varchar(255) NOT NULL,
    payload         jsonb NOT NULL,
    saga_status     saga_status NOT NULL,
    order_status    order_status NOT NULL,
    outbox_status   outbox_status NOT NULL,
    version         int NOT NULL
);
CREATE INDEX payment_outbox_outbox_status_saga_status
    ON "order".payment_outbox (type, outbox_status, saga_status);
CREATE UNIQUE INDEX payment_outbox_saga_id_saga_status
    ON "order".payment_outbox (type, saga_id, saga_status);
```

| Service        | Schema       |
|----------------|--------------|
| order-service  | `"order"` (quoted — reserved word) |
| payment-service| `payment`    |
| restaurant-service | `restaurant` |
| customer-service | (no outbox — read-only event consumer) |

The two indexes are critical:
- **Range scan** by `(type, outboxStatus, sagaStatus)` for the scheduler's polling query.
- **Unique** on `(type, sagaId, sagaStatus)` to enforce exactly-one outbox row per (saga, current state).

## Repository (Spring Data)

```java
public interface PaymentOutboxJpaRepository extends JpaRepository<PaymentOutboxEntity, UUID> {

    Optional<List<PaymentOutboxEntity>> findByTypeAndOutboxStatusAndSagaStatusIn(
        String type, OutboxStatus outboxStatus, SagaStatus... sagaStatuses);

    Optional<PaymentOutboxEntity> findByTypeAndSagaIdAndSagaStatusIn(
        String type, UUID sagaId, SagaStatus... sagaStatuses);

    void deleteByTypeAndOutboxStatusAndSagaStatusIn(
        String type, OutboxStatus outboxStatus, SagaStatus... sagaStatuses);
}
```

## Helper bean

Centralizes outbox CRUD + JSON (de)serialization:

```java
@Slf4j
@Component
public class PaymentOutboxHelper {

    private final PaymentOutboxJpaRepository repo;
    private final ObjectMapper mapper;
    private static final String TYPE = "payment";

    @Transactional(readOnly = true)
    public Optional<List<OrderPaymentOutboxMessage>> getPaymentOutboxMessageByOutboxStatusAndSagaStatus(
            final OutboxStatus outboxStatus, final SagaStatus... sagaStatuses) {
        return repo.findByTypeAndOutboxStatusAndSagaStatusIn(TYPE, outboxStatus, sagaStatuses)
            .map(list -> list.stream().map(this::toModel).toList());
    }

    @Transactional(readOnly = true)
    public Optional<OrderPaymentOutboxMessage> getPaymentOutboxMessageBySagaIdAndSagaStatus(
            final UUID sagaId, final SagaStatus... sagaStatuses) {
        return repo.findByTypeAndSagaIdAndSagaStatusIn(TYPE, sagaId, sagaStatuses).map(this::toModel);
    }

    @Transactional
    public void save(final OrderPaymentOutboxMessage msg) {
        final PaymentOutboxEntity entity = toEntity(msg);
        final PaymentOutboxEntity saved = repo.save(entity);
        if (saved == null) {
            throw new OrderDomainException("Could not save payment outbox: " + msg.getId());
        }
    }

    @Transactional
    public void savePaymentOutboxMessage(
            final OrderPaymentEventPayload payload,
            final OrderStatus orderStatus,
            final SagaStatus sagaStatus,
            final OutboxStatus outboxStatus,
            final UUID sagaId) {
        save(OrderPaymentOutboxMessage.builder()
            .id(UUID.randomUUID())
            .sagaId(sagaId)
            .createdAt(ZonedDateTime.now(ZoneId.of("UTC")))
            .type(TYPE)
            .payload(toJson(payload))
            .orderStatus(orderStatus)
            .sagaStatus(sagaStatus)
            .outboxStatus(outboxStatus)
            .build());
    }

    @Transactional
    public void deletePaymentOutboxMessageByOutboxStatusAndSagaStatus(
            final OutboxStatus outboxStatus, final SagaStatus... sagaStatuses) {
        repo.deleteByTypeAndOutboxStatusAndSagaStatusIn(TYPE, outboxStatus, sagaStatuses);
    }
}
```

## The Scheduler (relay)

```java
@Slf4j
@Component
@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentOutboxScheduler implements OutboxScheduler {

    private final PaymentOutboxHelper paymentOutboxHelper;
    private final PaymentRequestMessagePublisher publisher;

    @Override
    @Transactional
    @Scheduled(
        fixedDelayString  = "${order-service.outbox-scheduler-fixed-rate}",
        initialDelayString = "${order-service.outbox-scheduler-initial-delay}")
    public void processOutboxMessage() {
        final Optional<List<OrderPaymentOutboxMessage>> outboxMessages =
            paymentOutboxHelper.getPaymentOutboxMessageByOutboxStatusAndSagaStatus(
                OutboxStatus.STARTED, SagaStatus.STARTED, SagaStatus.COMPENSATING);

        outboxMessages.ifPresent(list -> {
            if (!list.isEmpty()) {
                log.info("Sending {} OrderPaymentOutboxMessage", list.size());
                list.forEach(m -> publisher.publish(m, this::updateOutboxStatus));
            }
        });
    }

    private void updateOutboxStatus(final OrderPaymentOutboxMessage msg, final OutboxStatus status) {
        msg.setOutboxStatus(status);
        paymentOutboxHelper.save(msg);
        log.info("PaymentOutbox table status updated: {}", status.name());
    }
}
```

Mandatory:
- `@Component` + implements `OutboxScheduler`.
- `@ConditionalOnProperty("scheduling.enabled", matchIfMissing = true)` — disable in tests by setting `scheduling.enabled: false`.
- `@Scheduled(fixedDelayString, initialDelayString)` reading from `<svc>-service.outbox-scheduler-*`.
- `@Transactional` on `processOutboxMessage`.
- The publish callback is responsible for transitioning `STARTED → COMPLETED|FAILED`.

## Configuration

```yaml
# application.yaml
scheduling:
  enabled: true

<svc>-service:
  outbox-scheduler-fixed-rate: 10000        # 10s polling
  outbox-scheduler-initial-delay: 10000     # 10s after startup
```

```yaml
# application-test.yaml — disable scheduling for deterministic tests
scheduling:
  enabled: false
```

The container module needs `@EnableScheduling` on `<Svc>Application` (or a `@Configuration` class).

## Cleanup scheduler (optional but recommended)

A second scheduler that purges `COMPLETED` rows past a TTL:

```java
@Component
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
public class PaymentOutboxCleanerScheduler implements OutboxScheduler {

    @Override
    @Transactional
    @Scheduled(cron = "@midnight")
    public void processOutboxMessage() {
        paymentOutboxHelper.deletePaymentOutboxMessageByOutboxStatusAndSagaStatus(
            OutboxStatus.COMPLETED, SagaStatus.SUCCEEDED);
    }
}
```

## Status transitions

```
                  insert (saga step or use case)
                          │
                          ▼
             ┌─────────────────────────┐
             │  outboxStatus=STARTED   │
             │  sagaStatus=STARTED     │
             └────────────┬────────────┘
                          │  scheduler picks up + publishes to Kafka
                          ▼
             ┌─────────────────────────┐  ack callback
             │  outboxStatus=COMPLETED │◄─────────────
             │  sagaStatus=STARTED     │
             └────────────┬────────────┘
                          │  inbound response triggers saga.process
                          │  outbox row updated by saga step
                          ▼
             ┌─────────────────────────┐
             │  sagaStatus=PROCESSING  │
             │  (or SUCCEEDED, …)      │
             └─────────────────────────┘
```

On Kafka publish failure: callback sets `outboxStatus=FAILED`. Scheduler should NOT auto-retry FAILED rows without manual intervention (dead-letter pattern).

## Why JSON payload (not Avro) in the outbox

The outbox stores the **payload object** (a plain Lombok class, e.g. `OrderPaymentEventPayload`) as JSON in a `jsonb` column. The Avro `*AvroModel` is built only at publish time by the publisher's mapper. This keeps the outbox decoupled from schema evolution.

### Payload class is NOT the domain event

This is a common confusion. The two classes live in different modules and serve different purposes:

| Class                     | Module                    | Purpose                                  | Holds            |
|---------------------------|---------------------------|------------------------------------------|------------------|
| `OrderCreatedEvent`       | `*-domain-core`           | Domain event raised by aggregate         | Full `Order` aggregate, no Jackson |
| `OrderPaymentEventPayload`| `*-application-service/.../outbox/model/payment/` | Flat DTO serialized into outbox `payload` | Primitive-ish fields + `@JsonProperty` |

```java
// order-domain-core/.../event/OrderCreatedEvent.java — pure domain
public class OrderCreatedEvent extends OrderEvent {
    public OrderCreatedEvent(Order order, ZonedDateTime createdAt) { super(order, createdAt); }
}
```

```java
// order-application-service/.../outbox/model/payment/OrderPaymentEventPayload.java — DTO
@Getter @Builder @AllArgsConstructor
public class OrderPaymentEventPayload {
    @JsonProperty private String        orderId;
    @JsonProperty private String        customerId;
    @JsonProperty private BigDecimal    price;
    @JsonProperty private ZonedDateTime createdAt;
    @JsonProperty private String        paymentOrderStatus;
}
```

A mapper bridges the two (e.g. `OrderDataMapper.orderCreatedEventToOrderPaymentEventPayload(OrderCreatedEvent)`). **Never serialize a domain event directly into the outbox** — it would drag the whole aggregate (and its non-Jackson dependencies) into the JSON column.

> Note: food-ordering-system uses Lombok classes (not Java `record`s) for payloads because of `@JsonProperty` placement and Builder needs. Either is fine in a new service; pick one and stay consistent.

## Integration test pattern (`@Sql` setup/cleanup)

food-ordering-system uses one canonical IT for the saga + outbox: `OrderPaymentSagaIT` in **`order-container`** (NOT in `*-data-access`). It uses `@Sql` to seed an outbox row in `STARTED` and to clean up afterwards:

```java
// order-container/src/test/java/.../outbox/OrderPaymentSagaIT.java
@Slf4j
@Sql(value = {"classpath:sql/OrderPaymentSagaTestSetUp.sql"})
@Sql(value = {"classpath:sql/OrderPaymentSagaTestCleanUp.sql"},
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class OrderPaymentSagaIT extends Bootstrap {

    @Autowired private OrderPaymentSaga             orderPaymentSaga;
    @Autowired private PaymentOutboxJpaRepository   paymentOutboxJpaRepository;

    @Test
    void it_should_completed_payment_but_try_again_the_payment_should_is_already_processed() {
        orderPaymentSaga.process(getPaymentResponse());
        orderPaymentSaga.process(getPaymentResponse());   // second call must be no-op
    }
}
```

The SQL files live next to the test:

- `order-container/src/test/resources/sql/OrderPaymentSagaTestSetUp.sql`
- `order-container/src/test/resources/sql/OrderPaymentSagaTestCleanUp.sql`

This is the right place because the test needs the full Spring container (JPA + scheduler beans + saga) wired with real Postgres via Testcontainers. Tests in `*-data-access` are usually pure repository tests with `@DataJpaTest`.

## Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Skip `@Version` | Always include — saga + scheduler race otherwise |
| Publish Kafka inside the use case transaction | Use outbox row instead |
| Keep `STARTED` rows forever after success | Mark `COMPLETED` then prune |
| Use a separate enum for outbox status | Use `com.lg5.spring.outbox.OutboxStatus` |
| Run scheduler in tests | Disable via `scheduling.enabled: false` (YAML only — there's no `@TestPropertySource` use of it in food-ordering-system) |
| Query outbox without `type` filter | Always include the discriminator |
| Add `@JdbcTypeCode(SqlTypes.JSON)` to the payload field | Leave it as `String`; rely on Postgres's `varchar→jsonb` implicit cast (matches food-ordering-system) |
| Serialize the domain event into the outbox | Map to a flat `*EventPayload` DTO first |
| Add `@Transactional` to the listener AND the helper | Pick one tx owner — usually the helper |
