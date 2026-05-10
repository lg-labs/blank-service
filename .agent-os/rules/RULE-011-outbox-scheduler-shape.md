---
id: RULE-011
slug: outbox-scheduler-shape
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: outbox
tags: [outbox, scheduler, scheduled, conditional-on-property, fixed-delay]
description: Outbox schedulers implement `com.lg5.spring.outbox.OutboxScheduler`, are annotated `@Scheduled(fixedDelayString = "${<svc>.outbox-scheduler-fixed-rate}")` and gated with `@ConditionalOnProperty("scheduling.enabled")`.
---

# RULE-011 — Outbox scheduler shape

## Statement

Every outbox-publishing scheduler is a Spring bean that:

1. **Implements** the framework interface `com.lg5.spring.outbox.OutboxScheduler`.
2. Is annotated `@Component` (or `@Service`) — see RULE-005, no custom
   annotation.
3. Has its `processOutboxMessage()` method annotated:
   ```java
   @Transactional
   @Scheduled(fixedDelayString = "${<svc>-service.outbox-scheduler-fixed-rate}")
   ```
4. Is gated at class level with:
   ```java
   @ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
   ```
   so that test profiles can disable scheduling by setting
   `scheduling.enabled: false` in `application-test.yaml`.
5. Reads `STARTED` outbox rows for its specific event type, publishes them to
   Kafka via the corresponding `KafkaProducerHelper`, and marks them
   `COMPLETED` (or `FAILED`) inside the same transaction.

## Rationale

The scheduler is the asynchronous half of the Transactional Outbox pattern
(RULE-008). Without the framework interface and the `@Scheduled` cadence:

- The outbox table grows unbounded (`STARTED` rows never published).
- Sagas stall (downstream services never receive the trigger event).

The `@ConditionalOnProperty` gate is what allows integration & ATDD tests to
run with the outbox table populated but without spurious scheduler ticks
firing during assertions. Disabling via property (not via
`@TestPropertySource` or profile activation) is the convention so that test
authors can flip it on for a single scenario when they want to assert the
publish path end-to-end.

The cadence (`fixed-delay` not `fixed-rate`) means a slow tick won't pile up;
the next tick starts X ms after the previous one finished, preventing thread
starvation during incident scenarios where the broker is slow.

## Example — correct

```java
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "scheduling.enabled", matchIfMissing = true)
@Slf4j
public class PaymentOutboxScheduler implements OutboxScheduler {

    private final PaymentOutboxHelper outboxHelper;
    private final PaymentRequestKafkaPublisher kafkaPublisher;

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${order-service.outbox-scheduler-fixed-rate}")
    public void processOutboxMessage() {
        final Optional<List<OrderPaymentOutboxMessage>> outbox =
            outboxHelper.getOrderPaymentOutboxMessageByOutboxStatusAndSagaStatus(
                OutboxStatus.STARTED, SagaStatus.STARTED, SagaStatus.COMPENSATING);

        outbox.ifPresent(messages -> {
            log.info("Publishing {} OrderPayment outbox messages", messages.size());
            messages.forEach(m -> kafkaPublisher.publish(m, this::updateOutboxStatus));
        });
    }

    private void updateOutboxStatus(final OrderPaymentOutboxMessage m, final OutboxStatus s) {
        m.setOutboxStatus(s);
        outboxHelper.save(m);
        log.info("Outbox {} updated to {}", m.getId(), s);
    }
}
```

```yaml
# payment-container/src/main/resources/application.yaml
order-service:
  outbox-scheduler-fixed-rate: 1000   # ms

# payment-container/src/test/resources/application-test.yaml
scheduling:
  enabled: false                      # disables ALL @Scheduled in test profile
```

## Anti-pattern

```java
// WRONG: no @ConditionalOnProperty → @Scheduled fires during ATDD/IT tests,
//        making assertions flaky.
@Component
public class PaymentOutboxScheduler implements OutboxScheduler {
    @Scheduled(fixedRate = 1000)                        // ❌ also: fixed-rate not fixed-delay
    public void processOutboxMessage() { ... }
}

// WRONG: hardcoded cadence (no property) — can't be tuned per-environment.
@Scheduled(fixedDelay = 1000)

// WRONG: doesn't implement OutboxScheduler — loses the framework's contract
//        that the saga orchestrator relies on for type-safe scheduling.
@Component
public class PaymentOutboxScheduler {
    @Scheduled(fixedDelayString = "${...}")
    public void tick() { ... }
}
```

## References

- Skill: `lg5-outbox` (full scheduler walkthrough, including the
  `OutboxStatus`/`SagaStatus` query patterns).
- Framework interface: `com.lg5.spring.outbox.OutboxScheduler`.
- Related rules: RULE-008 (outbox table), RULE-012 (test profile activation),
  RULE-014 (configuration prefixes).
