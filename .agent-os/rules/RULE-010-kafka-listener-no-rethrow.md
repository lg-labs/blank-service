---
id: RULE-010
slug: kafka-listener-no-rethrow
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: kafka
tags: [kafka, listener, batch-listener, optimistic-locking, no-op, redelivery]
description: Kafka listeners are batch by default (`batch-listener: true`). They must catch `OptimisticLockingFailureException` and not-found exceptions as NO-OP (do not rethrow) to prevent infinite Kafka redelivery loops.
---

# RULE-010 — Kafka listener exception handling (NO-OP for optimistic-lock and not-found)

## Statement

Kafka listeners in consumer services follow these rules:

1. **Batch listeners by default**: configure
   `kafka-consumer-config.batch-listener: true` and accept
   `List<ConsumerRecord<K, V>>` (or `List<V>`) in the listener method.
2. **Inside the listener**, catch the following exceptions and **swallow them
   as NO-OP** (log at WARN, do not rethrow):
   - `org.springframework.dao.OptimisticLockingFailureException` — happens
     when a saga step retries and a competing thread already advanced the
     outbox row.
   - Domain not-found exceptions for the relevant aggregate
     (e.g. `PaymentNotFoundException`, `OrderNotFoundException`) — happens
     when the producer's outbox arrives before the consumer's local read
     model has the entity, and the saga's later retry will succeed.
3. **All other exceptions propagate** so that the framework's error handler
   (`Lg5SeekToCurrentErrorHandler` or your custom one) can dead-letter or
   retry as configured.

Never wrap the entire batch in a single try/catch that swallows everything
— that hides genuine bugs.

## Rationale

Kafka delivers at-least-once. The framework's outbox scheduler and the
saga step's idempotency guard (RULE-009) are designed to make duplicate
deliveries safe, but the **bridge** between Kafka redelivery and that
guard is the listener's exception handling.

If the listener rethrows `OptimisticLockingFailureException`, Kafka treats
the batch as failed, redelivers it, the same race happens again, and the
service enters an **infinite redelivery loop**. The same applies for the
not-found case while the producer-consumer state is converging.

By catching these two exception types as NO-OP, the listener acknowledges
"yes, I received this message, no work to do, move on" — which is exactly
what the consumer should communicate when the saga step has already been
processed by another concurrent attempt or when the local read model is
about to be updated by another path.

## Example — correct

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResponseKafkaListener implements KafkaConsumer<PaymentResponseAvroModel> {

    private final PaymentResponseMessageListener messageListener;

    @Override
    @KafkaListener(
        id = "${kafka-consumer-config.payment-consumer-group-id}",
        topics = "${order-service.payment-response-topic-name}"
    )
    public void receive(
        @Payload final List<PaymentResponseAvroModel> messages,
        @Header(KafkaHeaders.RECEIVED_KEY)        final List<String>  keys,
        @Header(KafkaHeaders.RECEIVED_PARTITION)  final List<Integer> partitions,
        @Header(KafkaHeaders.OFFSET)              final List<Long>    offsets) {

        log.info("Received {} payment responses", messages.size());
        messages.forEach(this::handleOne);
    }

    private void handleOne(final PaymentResponseAvroModel msg) {
        try {
            messageListener.paymentCompleted(toDomain(msg));
        } catch (final OptimisticLockingFailureException e) {
            log.warn("OptimisticLock for sagaId={}, treating as NO-OP", msg.getSagaId(), e);
        } catch (final OrderNotFoundException e) {
            log.warn("Order {} not found yet, NO-OP — saga will retry", msg.getOrderId(), e);
        }
    }
}
```

## Anti-pattern

```java
// WRONG: rethrows OptimisticLock → infinite Kafka redelivery loop
@KafkaListener(...)
public void receive(final List<PaymentResponseAvroModel> messages) {
    messages.forEach(messageListener::paymentCompleted);   // ❌ no try/catch
}

// WRONG: swallows EVERY exception, hiding real bugs
@KafkaListener(...)
public void receive(final List<PaymentResponseAvroModel> messages) {
    try {
        messages.forEach(messageListener::paymentCompleted);
    } catch (final Exception e) {
        log.warn("Something went wrong", e);                // ❌ too broad
    }
}
```

## References

- Skill: `lg5-kafka-avro` (full producer/consumer recipe).
- Skill: `lg5-saga` (how the listener-NO-OP pattern interacts with saga
  idempotency).
- Related rules: RULE-008 (outbox `@Version` is what raises the
  `OptimisticLockingFailureException` in the first place), RULE-009 (saga
  step idempotency guard reduces but doesn't eliminate the lock conflicts).
