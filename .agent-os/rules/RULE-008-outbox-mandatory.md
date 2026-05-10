---
id: RULE-008
slug: outbox-mandatory
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: outbox
tags: [outbox, transactional-outbox, optimistic-locking, kafka, atomicity]
description: Every domain event that crosses a service boundary must be published via the Transactional Outbox. The outbox JPA entity must carry `@Version` (optimistic locking) and an `OutboxStatus` field (STARTED|COMPLETED|FAILED).
---

# RULE-008 — Transactional Outbox is mandatory

## Statement

Any domain event that leaves the service (Kafka, HTTP webhook, anything
asynchronous to another bounded context) must be persisted to a dedicated
**outbox table** in the **same database transaction** as the business
state change. A scheduler then publishes the row to the message broker and
marks it `COMPLETED`.

Concretely, every outbox JPA entity must have:

```java
@Entity
@Table(name = "<event>_outbox", schema = "\"<service-schema>\"")
public class <Event>OutboxEntity {
    @Id              private UUID id;
    @Version         private int version;                 // ❶ optimistic locking — REQUIRED
    @Enumerated(EnumType.STRING)
    private OutboxStatus outboxStatus;                    // ❷ STARTED | COMPLETED | FAILED — REQUIRED
    @Enumerated(EnumType.STRING)
    private SagaStatus    sagaStatus;                     // when participating in a saga
    private UUID    sagaId;
    private String  type;                                 // event name, e.g. "OrderPaid"
    private String  payload;                              // JSON string; jsonb at DDL level
    private ZonedDateTime createdAt;
    private ZonedDateTime processedAt;                    // set when COMPLETED
}
```

Constraints to add at DDL level:

- range index on `(type, outbox_status, saga_status)` for the scheduler poll;
- unique index on `(type, saga_id, saga_status|payment_status)` to enforce
  idempotent saga step persistence;
- `outbox_status` and `saga_status` as native Postgres ENUMs (mapped to
  `EnumType.STRING` in JPA — implicit cast handles the bridge).

Helper-class pattern: a thin `*MessageListenerImpl`/`*CommandHandler` (no
`@Transactional`) delegates to a `*Helper.persistX(@Transactional)` method
that writes the business row + the outbox row in a single transaction.

## Rationale

Without the outbox, "save business state then publish to Kafka" is two
independent operations that can fail independently — leading to **lost
events** (DB committed, Kafka publish failed) or **phantom events** (Kafka
published, DB rolled back). The outbox makes the two atomic by deferring
publication to a separate transaction that reads from the SAME database.

The `@Version` field is **non-negotiable**: when a saga step retries (e.g.
because of a Kafka redelivery or a competing scheduler tick), two concurrent
updates to the same outbox row would otherwise overwrite each other. With
`@Version`, the second loser receives `OptimisticLockingFailureException`,
which the listener swallows as NO-OP (see RULE-010), and the duplicate
processing terminates safely.

The `OutboxStatus` field lets the scheduler poll only `STARTED` rows and the
saga query only `COMPLETED` rows for downstream reads, preventing race
conditions during the publish window.

## Example — correct

```java
// payment-application-service/.../outbox/model/OrderOutboxEntity.java
@Entity
@Table(name = "order_outbox", schema = "\"payment\"")
@Getter @Setter @Builder
public class OrderOutboxEntity {
    @Id                                       private UUID id;
    @Version                                  private int version;
    @Enumerated(EnumType.STRING)              private OutboxStatus outboxStatus;
    @Enumerated(EnumType.STRING)              private PaymentStatus paymentStatus;
    private UUID sagaId;
    private String type;
    private String payload;                   // JSON string of OrderEventPayload
    private ZonedDateTime createdAt;
    private ZonedDateTime processedAt;
}
```

```java
// PaymentRequestHelper.java
@Component @RequiredArgsConstructor
public class PaymentRequestHelper {

    @Transactional
    public Payment persistPayment(final PaymentRequest request) {
        final Payment payment = paymentRepository.save(...);   // business row
        outboxHelper.save(toOutbox(payment, OutboxStatus.STARTED, ...)); // outbox row
        return payment;                                        // single Tx
    }
}
```

## Anti-pattern

```java
// WRONG: publish to Kafka inside the business transaction
@Transactional
public void create(final PlaceOrder cmd) {
    final Order order = orderRepository.save(Order.from(cmd));
    kafkaProducer.send("order-created", new OrderCreatedEvent(order));   // ❌
}
// If Kafka is down, the @Transactional doesn't roll back the Kafka send
// (it's not transactional), so you can also lose the Kafka send if the DB
// commit fails AFTER it. Either way, atomicity is gone.
```

```java
// WRONG: outbox entity without @Version
@Entity
public class OrderOutboxEntity {
    @Id private UUID id;
    @Enumerated(EnumType.STRING) private OutboxStatus outboxStatus;
    // ❌ missing @Version int version — saga retries will silently overwrite
}
```

## References

- Skill: `lg5-outbox` (full DDL + entity + helper + scheduler walkthrough,
  including the JPA-vs-DDL asymmetry: `String` payload + `EnumType.STRING`
  in JPA, `jsonb` + native PG enums in DDL).
- Reference: food-ordering-system order-service `OrderOutboxEntity`,
  `PaymentOutboxEntity`, `RestaurantApprovalOutboxEntity`.
- Related rules: RULE-009 (saga step idempotency uses outbox), RULE-010
  (listener swallows OptimisticLockingFailureException), RULE-011 (scheduler).
