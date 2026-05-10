---
description: Add a batch Kafka listener with NO-OP exception handling per RULE-010 (swallows OptimisticLockingFailureException + not-found exceptions; rethrows everything else).
argument-hint: <service-name> <topic-name> <avro-model-name>
allowed-tools: bash, read, write, edit, glob, grep
---

# /add-kafka-listener

You are adding a Kafka consumer to a lg5-spring microservice. The listener
follows the framework's batch pattern with the canonical NO-OP exception
handling that prevents infinite Kafka redelivery loops.

Follow RULE-007 and RULE-010 strictly.

## Inputs

- `<service-name>` — the existing service.
- `<topic-name>` — the Kafka topic to subscribe to (e.g. `payment-response`).
- `<avro-model-name>` — the Avro generated class name (e.g.
  `PaymentResponseAvroModel`). Must already exist under
  `<service-name>-message-model/src/main/resources/avro/<avro-model-name>.avsc`.

If the user provided fewer arguments, ask BEFORE writing files. If the
`.avsc` does not exist yet, ask the user to define its fields and create the
schema first (then run `make run-avro-model`).

## Pre-flight checks

1. The service has the canonical 8-module shape (RULE-004).
2. The Avro schema file exists; the generated class compiles
   (`make install-skip-test` succeeds).
3. The consumer group id is decided. Convention:
   `<service-name>-<topic-name>-consumer`.
4. Determine which not-found domain exceptions to swallow as NO-OP. By
   default, the listener catches `OptimisticLockingFailureException` only;
   the user must enumerate any domain-specific not-found exceptions to add.

## Steps

1. **Listener interface** at
   `<service-name>-domain-core/.../ports/input/message/listener/<TopicNameCamel>MessageListener.java`:
   ```java
   public interface <TopicNameCamel>MessageListener {
       void handle(<TopicNameCamel>Response response);
   }
   ```
   (input port — pure Java, no Spring, RULE-003).

2. **Listener impl (helper-routed)** at
   `<service-name>-application-service/.../ports/input/message/listener/<TopicNameCamel>MessageListenerImpl.java`:
   ```java
   @Service
   @RequiredArgsConstructor
   @Slf4j
   public class <TopicNameCamel>MessageListenerImpl implements <TopicNameCamel>MessageListener {
       private final <TopicNameCamel>Helper helper;
       @Override
       public void handle(final <TopicNameCamel>Response response) {
           helper.persist(response);                        // ← @Transactional inside helper
       }
   }
   ```
   No `@Transactional` on the impl itself — the helper owns the boundary
   (helper-class pattern, RULE-008).

3. **Helper** at
   `<service-name>-application-service/.../<TopicNameCamel>Helper.java`:
   ```java
   @Component
   @RequiredArgsConstructor
   public class <TopicNameCamel>Helper {
       @Transactional
       public void persist(final <TopicNameCamel>Response response) {
           // 1. Outbox-by-(sagaId, status) idempotency guard if part of saga (RULE-009).
           // 2. Load aggregate, apply event, save aggregate, save outbox.
       }
   }
   ```

4. **Mapper** at `<service-name>-message-core/.../mapper/<TopicNameCamel>MessagingDataMapper.java`:
   - `<TopicNameCamel>Response toDomain(<avro-model-name> avro)`.
   - Uses MapStruct or hand-written; no Spring annotations beyond `@Component`.

5. **Kafka listener** at
   `<service-name>-message-core/.../listener/<TopicNameCamel>KafkaListener.java`:
   ```java
   @Component
   @RequiredArgsConstructor
   @Slf4j
   public class <TopicNameCamel>KafkaListener implements KafkaConsumer<<avro-model-name>> {

       private final <TopicNameCamel>MessageListener  listener;
       private final <TopicNameCamel>MessagingDataMapper mapper;

       @Override
       @KafkaListener(
           id = "${kafka-consumer-config.<service-name>-<topic-name>-consumer-group-id}",
           topics = "${<service-name>-service.<topic-name>-topic-name}"
       )
       public void receive(
           @Payload final List<<avro-model-name>> messages,
           @Header(KafkaHeaders.RECEIVED_KEY)        final List<String>  keys,
           @Header(KafkaHeaders.RECEIVED_PARTITION)  final List<Integer> partitions,
           @Header(KafkaHeaders.OFFSET)              final List<Long>    offsets) {

           log.info("Received {} <topic-name> messages", messages.size());
           messages.forEach(this::handleOne);
       }

       private void handleOne(final <avro-model-name> msg) {
           try {
               listener.handle(mapper.toDomain(msg));
           } catch (final OptimisticLockingFailureException e) {
               log.warn("OptimisticLock for sagaId={}, NO-OP", msg.getSagaId(), e);
           } catch (final <NotFoundException> e) {                  // per user-provided list
               log.warn("Not found {}, NO-OP — will retry", e.getMessage());
           }
       }
   }
   ```
   Batch listener (`@Payload List<...>`) per RULE-010.

6. **application.yaml updates** in
   `<service-name>-container/src/main/resources/application.yaml`:
   ```yaml
   kafka-consumer-config:
     batch-listener: true                                    # RULE-010
     <service-name>-<topic-name>-consumer-group-id: <service-name>-<topic-name>-consumer
   <service-name>-service:
     <topic-name>-topic-name: <topic-name>
   ```
   (canonical prefixes per RULE-014).

7. **IT** at `<service-name>-container/src/test/java/.../<TopicNameCamel>KafkaListenerIT.java`
   extending `Lg5TestBoot[PortNone]` (RULE-012), with
   `testcontainers.kafka.enabled: true` and `testcontainers.schema-registry.enabled: true`
   (RULE-013). Assert:
   - happy path consumption updates the aggregate;
   - `OptimisticLockingFailureException` is swallowed as NO-OP (no
     redelivery storm);
   - the not-found exception is swallowed as NO-OP.

8. **Sanity build**: `make install-skip-test`.

9. **Final report**:
   - files created (paths + role);
   - the consumer group id wired;
   - reminder of the helper-class invariant: thin listener → helper owns
     `@Transactional` (RULE-008 helper pattern);
   - reminder of the NO-OP scope: ONLY `OptimisticLockingFailureException`
     and the user-listed not-found exceptions are swallowed; everything else
     propagates to `Lg5SeekToCurrentErrorHandler` for retry/DLQ (RULE-010).

## Anti-patterns to avoid

- DO NOT use a single-message listener (`@Payload <avro-model-name>` instead
  of `List<...>`) — the framework default is batch (RULE-010).
- DO NOT wrap the entire batch in one try/catch that swallows everything —
  hides real bugs (RULE-010 anti-pattern).
- DO NOT put `@Transactional` on the listener impl itself — the helper owns
  the boundary (RULE-008).
- DO NOT omit the NO-OP try/catch — uncaught `OptimisticLockingFailureException`
  causes infinite Kafka redelivery (RULE-010).
- DO NOT consume a JSON or Java-serialized payload — Avro only (RULE-007).
- DO NOT bypass the canonical config prefixes (RULE-014).

## References

- Skill: `lg5-kafka-avro` (full producer + consumer recipe with Avro).
- Skill: `food-ordering-system` (real listener examples:
  `PaymentResponseKafkaListener`, `RestaurantApprovalResponseKafkaListener`).
- Rules: RULE-003, RULE-007, RULE-008, RULE-010, RULE-012, RULE-013, RULE-014.
