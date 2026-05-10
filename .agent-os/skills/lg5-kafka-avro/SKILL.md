---
name: lg5-kafka-avro
version: 0.1.0
lg5-spring-sha: d0d754a
last-validated: 2026-05-09
description: How to wire Kafka producers, consumers and Avro schemas in lg5-spring services using KafkaProducer<K,V>, KafkaConsumer<T>, KafkaMessageHelper, the Avro maven plugin and the kafka-* configuration prefixes. Load this skill when the user asks about Kafka publishers, listeners, batch consumption, schema registry, Avro generation, or topic configuration.
---

# lg5-spring — Kafka Producer / Consumer + Avro

> Framework modules: `lg5-spring-kafka-config`, `lg5-spring-kafka-producer`, `lg5-spring-kafka-consumer`, `lg5-spring-kafka-model`.
> Reference impl: `/tmp/lg5-study/food-ordering-system/order-service/order-message/`.

## Hard rules

1. **All payloads are Avro `SpecificRecordBase`**. Producers and consumers are generic over `<K extends Serializable, V extends SpecificRecordBase>`.
2. **Schemas live in** `<svc>-message-model/src/main/resources/avro/*.avsc`.
3. **Schema namespace** convention: `<base.pkg>.message.model.avro`.
4. **Topic names** come from `<svc>-service.*-topic-name` properties — never hard-code.
5. **Listeners are batch** by default (`batch-listener: true`) — receive `List<V>`.
6. **NO-OP exception swallowing** in listeners for `OptimisticLockingFailureException` and not-found exceptions, to prevent Kafka redelivery loops.
7. **Schema registry** is mandatory (`io.confluent.kafka.serializers.KafkaAvroSerializer/Deserializer`).

## Avro schema (.avsc)

```json
// <svc>-message-model/src/main/resources/avro/payment_request.avsc
{
  "namespace": "com.labs.lg.food.ordering.system.message.model.avro",
  "type": "record",
  "name": "PaymentRequestAvroModel",
  "fields": [
    { "name": "id",         "type": "string" },
    { "name": "sagaId",     "type": "string" },
    { "name": "customerId", "type": "string" },
    { "name": "orderId",    "type": "string" },
    { "name": "price",
      "type": { "type": "bytes", "logicalType": "decimal", "precision": 10, "scale": 2 } },
    { "name": "createdAt",
      "type": { "type": "long", "logicalType": "timestamp-millis" } },
    { "name": "paymentOrderStatus",
      "type": { "type": "enum",
                "name": "PaymentOrderStatus",
                "symbols": ["PENDING", "CANCELLED"] } }
  ]
}
```

Regenerate Java classes:
```bash
make run-avro-model           # blank-service
make run-kafka-model          # food-ordering-system
# or directly:
mvn -pl <svc>-message/<svc>-message-model clean install
```

The Avro maven plugin is configured in `<svc>-message-model/pom.xml`. Since LG-71 (`fd9495b`), `testSourceDirectory` is also wired, so `src/test/avro` regenerates into `target/generated-test-sources`.

## Producer

### Bean wiring (per service)

The framework provides `KafkaProducerImpl`. Just declare the right `KafkaTemplate` bean:

```java
@Configuration
public class OrderKafkaProducerConfig {

    private final KafkaConfigData kafkaConfigData;
    private final KafkaProducerConfigData producerConfigData;

    @Bean
    public ProducerFactory<String, PaymentRequestAvroModel> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfig());
    }

    @Bean
    public KafkaTemplate<String, PaymentRequestAvroModel> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    private Map<String, Object> producerConfig() {
        final Map<String, Object> p = new HashMap<>();
        p.put(BOOTSTRAP_SERVERS_CONFIG,    kafkaConfigData.getBootstrapServers());
        p.put(kafkaConfigData.getSchemaRegistryUrlKey(), kafkaConfigData.getSchemaRegistryUrl());
        p.put(KEY_SERIALIZER_CLASS_CONFIG,   producerConfigData.getKeySerializerClass());
        p.put(VALUE_SERIALIZER_CLASS_CONFIG, producerConfigData.getValueSerializerClass());
        p.put(BATCH_SIZE_CONFIG,
            producerConfigData.getBatchSize() * producerConfigData.getBatchSizeBoostFactor());
        p.put(LINGER_MS_CONFIG,         producerConfigData.getLingerMs());
        p.put(COMPRESSION_TYPE_CONFIG,  producerConfigData.getCompressionType());
        p.put(ACKS_CONFIG,              producerConfigData.getAcks());
        p.put(REQUEST_TIMEOUT_MS_CONFIG, producerConfigData.getRequestTimeoutMs());
        p.put(RETRIES_CONFIG,           producerConfigData.getRetryCount());
        return p;
    }
}
```

### Publisher (output port adapter)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaymentEventKafkaPublisher implements PaymentRequestMessagePublisher {

    private final OrderMessagingDataMapper mapper;
    private final KafkaProducer<String, PaymentRequestAvroModel> kafkaProducer;
    private final OrderServiceConfigData configData;
    private final KafkaMessageHelper kafkaMessageHelper;

    @Override
    public void publish(
            final OrderPaymentOutboxMessage outboxMessage,
            final BiConsumer<OrderPaymentOutboxMessage, OutboxStatus> outboxCallback) {

        final OrderPaymentEventPayload payload =
            kafkaMessageHelper.stringToObjectClass(outboxMessage.getPayload(),
                                                   OrderPaymentEventPayload.class);
        final String sagaId = outboxMessage.getSagaId().toString();

        try {
            final PaymentRequestAvroModel avroModel =
                mapper.orderPaymentEventToPaymentRequestAvroModel(sagaId, payload);

            kafkaProducer.send(
                configData.getPaymentRequestTopicName(),
                sagaId,
                avroModel,
                kafkaMessageHelper.getKafkaCallback(
                    configData.getPaymentRequestTopicName(),
                    avroModel,
                    outboxMessage,
                    outboxCallback,
                    payload.getOrderId()));
        } catch (Exception e) {
            log.error("Error sending PaymentRequestAvroModel for sagaId {}: {}", sagaId, e.getMessage());
        }
    }
}
```

`KafkaMessageHelper.getKafkaCallback(topic, avro, outboxMessage, outboxCallback, eventId)`:
- on success → `outboxCallback.accept(outboxMessage, OutboxStatus.COMPLETED)`
- on failure → `outboxCallback.accept(outboxMessage, OutboxStatus.FAILED)` + log

## Consumer

### Bean wiring

```java
@Configuration
public class OrderKafkaConsumerConfig {

    private final KafkaConfigData kafkaConfigData;
    private final KafkaConsumerConfigData consumerConfigData;

    @Bean
    public ConsumerFactory<String, PaymentResponseAvroModel> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfig());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentResponseAvroModel>
            kafkaListenerContainerFactory() {

        final var factory = new ConcurrentKafkaListenerContainerFactory<String, PaymentResponseAvroModel>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(consumerConfigData.getBatchListener());
        factory.setConcurrency(consumerConfigData.getConcurrencyLevel());
        factory.setAutoStartup(consumerConfigData.getAutoStartup());
        factory.getContainerProperties().setPollTimeout(consumerConfigData.getPollTimeoutMs());
        return factory;
    }

    private Map<String, Object> consumerConfig() {
        final Map<String, Object> c = new HashMap<>();
        c.put(BOOTSTRAP_SERVERS_CONFIG,            kafkaConfigData.getBootstrapServers());
        c.put(kafkaConfigData.getSchemaRegistryUrlKey(), kafkaConfigData.getSchemaRegistryUrl());
        c.put(KEY_DESERIALIZER_CLASS_CONFIG,       consumerConfigData.getKeyDeserializer());
        c.put(VALUE_DESERIALIZER_CLASS_CONFIG,     consumerConfigData.getValueDeserializer());
        c.put(consumerConfigData.getSpecificAvroReaderKey(), consumerConfigData.getSpecificAvroReader());
        c.put(AUTO_OFFSET_RESET_CONFIG,            consumerConfigData.getAutoOffsetReset());
        c.put(SESSION_TIMEOUT_MS_CONFIG,           consumerConfigData.getSessionTimeoutMs());
        c.put(MAX_POLL_RECORDS_CONFIG,             consumerConfigData.getMaxPollRecords());
        return c;
    }
}
```

### Listener (input port adapter)

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResponseKafkaListener implements KafkaConsumer<PaymentResponseAvroModel> {

    private final PaymentResponseMessageListener paymentResponseMessageListener;
    private final OrderMessagingDataMapper mapper;

    @Override
    @KafkaListener(
        id    = "${kafka-consumer-config.payment-consumer-group-id}",
        topics = "${order-service.payment-response-topic-name}")
    public void receive(
            @Payload final List<PaymentResponseAvroModel> messages,
            @Header(KafkaHeaders.RECEIVED_KEY)       final List<String>  keys,
            @Header(KafkaHeaders.RECEIVED_PARTITION) final List<Integer> partitions,
            @Header(KafkaHeaders.OFFSET)             final List<Long>    offsets) {

        log.info("{} payment responses received with keys {}, partitions {}, offsets {}",
            messages.size(), keys, partitions, offsets);

        messages.forEach(msg -> {
            try {
                if (PaymentStatus.COMPLETED == msg.getPaymentStatus()) {
                    paymentResponseMessageListener.paymentCompleted(mapper.toResponse(msg));
                } else if (PaymentStatus.CANCELLED == msg.getPaymentStatus()
                        || PaymentStatus.FAILED  == msg.getPaymentStatus()) {
                    paymentResponseMessageListener.paymentCancelled(mapper.toResponse(msg));
                }
            } catch (OptimisticLockingFailureException e) {
                // NO-OP — another instance won the race; do NOT rethrow.
                log.error("Caught OptimisticLockingFailureException for orderId {}: {}",
                    msg.getOrderId(), e.getMessage());
            } catch (OrderNotFoundException e) {
                // NO-OP — order doesn't exist (already deleted/never existed).
                log.error("OrderNotFoundException for orderId {}: {}",
                    msg.getOrderId(), e.getMessage());
            }
        });
    }
}
```

The two `catch` blocks are **load-bearing**: rethrowing would cause Kafka to redeliver the same batch endlessly.

## Configuration (application.yaml)

```yaml
order-service:
  payment-request-topic-name:  payment-request
  payment-response-topic-name: payment-response
  restaurant-approval-request-topic-name:  restaurant-approval-request
  restaurant-approval-response-topic-name: restaurant-approval-response

kafka-config:
  bootstrap-servers: localhost:19092,localhost:29092,localhost:39092
  schema-registry-url-key: schema.registry.url
  schema-registry-url: http://localhost:8081
  num-of-partitions: 3
  replication-factor: 3

kafka-producer-config:
  key-serializer-class:   org.apache.kafka.common.serialization.StringSerializer
  value-serializer-class: io.confluent.kafka.serializers.KafkaAvroSerializer
  compression-type: snappy
  acks: all
  batch-size: 16384
  batch-size-boost-factor: 100
  linger-ms: 5
  request-timeout-ms: 60000
  retry-count: 5

kafka-consumer-config:
  key-deserializer:   org.apache.kafka.common.serialization.StringDeserializer
  value-deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
  payment-consumer-group-id: payment-topic-consumer
  restaurant-approval-consumer-group-id: restaurant-approval-topic-consumer
  auto-offset-reset: earliest
  specific-avro-reader-key: specific.avro.reader
  specific-avro-reader: true
  batch-listener: true
  auto-startup: true
  concurrency-level: 3
  session-timeout-ms: 10000
  heartbeat-interval-ms: 3000
  max-poll-interval-ms: 300000
  max-poll-records: 500
  max-partition-fetch-bytes-default: 1048576
  max-partition-fetch-bytes-boost-factor: 1
  poll-timeout-ms: 150
```

## Topic auto-creation

Implement `KafkaAdminClient` bean per service to create topics on startup with the configured partitions/replication-factor. (See `food-ordering-system/.../KafkaAdminClient.java`.)

## Mapper layer

Always introduce a `<Svc>MessagingDataMapper` `@Component` between the Avro generated class and the domain payload record:

```java
@Component
public class OrderMessagingDataMapper {

    public PaymentRequestAvroModel orderPaymentEventToPaymentRequestAvroModel(
            final String sagaId, final OrderPaymentEventPayload payload) {
        return PaymentRequestAvroModel.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setSagaId(sagaId)
            .setCustomerId(payload.getCustomerId())
            .setOrderId(payload.getOrderId())
            .setPrice(payload.getPrice())
            .setCreatedAt(payload.getCreatedAt().toInstant())
            .setPaymentOrderStatus(PaymentOrderStatus.valueOf(payload.getPaymentOrderStatus()))
            .build();
    }

    public PaymentResponse toResponse(final PaymentResponseAvroModel m) {
        return PaymentResponse.builder()
            .id(m.getId())
            .sagaId(m.getSagaId())
            .paymentId(m.getPaymentId())
            .customerId(m.getCustomerId())
            .orderId(m.getOrderId())
            .price(m.getPrice())
            .createdAt(m.getCreatedAt())
            .paymentStatus(com.…domain.valueobject.PaymentStatus.valueOf(m.getPaymentStatus().name()))
            .failureMessages(m.getFailureMessages())
            .build();
    }
}
```

This isolates Avro types from the domain — the domain never imports `*AvroModel`.

## Testing

- **Producer unit tests**: mock `KafkaProducer<K,V>` + `KafkaMessageHelper`, assert `send()` arguments.
- **Consumer integration**: use `Lg5TestBoot` + `KafkaContainerCustomConfig`. Send via real `KafkaTemplate` to the listener topic, await DB side-effect.
- **Schema-registry**: in tests it's spun up by `KafkaContainerCustomConfig` (LG-71 added schema-registry support to the same container config).

## Listener → Helper delegation (do NOT make the listener `@Transactional`)

food-ordering-system keeps the Kafka listener annotation-free regarding transactions. The listener catches batch-level exceptions, then dispatches each message to a thin `*MessageListener` impl that delegates to a `@Transactional` helper. The transaction owner is always the **helper**, never the Kafka listener.

```
@KafkaListener (no @Transactional)
   └── catches OptimisticLockingFailure / NotFound  → NO-OP per message
   └── for each msg: messageListener.completePayment(req)        // thin
                          └── helper.persistPayment(req)         // @Transactional
                                  ├── idempotency lookup on outbox
                                  ├── domain mutation + JPA save
                                  └── outboxHelper.saveOutboxMessage(...)
```

This separation is critical:

- The `OptimisticLockingFailureException` thrown inside the helper transaction propagates *out* of the transaction (so Spring rolls back), is then caught by the listener, and becomes a logged NO-OP. If the listener were `@Transactional`, the rollback semantics would be wrong (one bad message in a batch could roll back the whole listener-level tx).
- `@TransactionalEventListener` is **not used** in food-ordering-system (despite the presence of `ApplicationEventDomainPublisher`, which is dead code — nothing listens to its events). The outbox row is written **synchronously inside the helper transaction**, immediately after the JPA save. See `lg5-saga` skill for the helper-class pattern in detail.

## Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Use JSON serializer | Always Avro + schema registry |
| Hard-code topic names | Use `${<svc>-service.*-topic-name}` |
| Rethrow `OptimisticLockingFailureException` from listener | NO-OP + log |
| Single-record listener (`@Payload V msg`) | Batch listener (`List<V>`) |
| Skip the mapper, leak Avro into domain | Always go through `*MessagingDataMapper` |
| Publish directly from use case | Use Outbox + Scheduler (see `lg5-outbox`) |
| Forget `@CreationTimestamp` style on Avro `createdAt` | Set explicitly in mapper from payload |
| Put `@Transactional` on the `@KafkaListener` method | Tx owner is the helper invoked from the message-listener impl |
| Wire `ApplicationEventDomainPublisher` and expect outbox magic | Write the outbox row synchronously in the command handler / helper |
