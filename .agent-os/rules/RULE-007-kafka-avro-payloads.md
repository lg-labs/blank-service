---
id: RULE-007
slug: kafka-avro-payloads
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: kafka
tags: [kafka, avro, schema-registry, message-model, specific-record-base]
description: Every Kafka producer and consumer is generic over `V extends SpecificRecordBase`. Avro schemas live in `<svc>-message-model/src/main/resources/avro/*.avsc` and are regenerated with `make run-avro-model`.
---

# RULE-007 — Kafka payloads must be Avro-typed

## Statement

All Kafka traffic in lg5-spring services uses **Avro** (Confluent Schema
Registry) end-to-end. Concretely:

- Every producer/consumer wrapper is generic over `V extends SpecificRecordBase`.
- Schemas live in `<svc>-message-model/src/main/resources/avro/*.avsc`, one
  schema per file, named after the message type (e.g. `PaymentRequestAvroModel.avsc`).
- Generated classes land in `<svc>-message-model/target/generated-sources/avro/`
  via `make run-avro-model` (or `make run-kafka-model` in food-ordering-system).
- The Kafka **key** for saga-related topics is always the saga id (string).
- Producer/consumer factories live in `<svc>-message-core/.../config/`.

Never publish JSON, never use a `Map<String, Object>`, never use Java
serialization, never put the Avro schemas anywhere outside
`*-message-model/src/main/resources/avro/`.

## Rationale

Avro + Schema Registry gives the framework three properties for free:

1. **Schema evolution with backward/forward compatibility** enforced at
   registration time, so consumers don't break when producers add an optional
   field.
2. **Compact binary on the wire** (smaller than JSON, faster to deserialize).
3. **A single source of truth** — the `.avsc` file is the contract; both
   producer and consumer derive their types from it. There is never drift
   between "what the producer thinks the message looks like" and "what the
   consumer expects".

The `SpecificRecordBase` generic constraint means the framework's helper
classes (`KafkaProducerHelper<K, V>`, `KafkaMessageHelper<K, V>`) can publish
ANY message type without runtime reflection or per-message boilerplate.

## Example — correct

```
payment-message-model/
└── src/main/resources/avro/
    ├── PaymentRequestAvroModel.avsc
    └── PaymentResponseAvroModel.avsc
```

```json
// payment-message-model/src/main/resources/avro/PaymentRequestAvroModel.avsc
{
  "type": "record",
  "name": "PaymentRequestAvroModel",
  "namespace": "com.example.payment.kafka.avro.model",
  "fields": [
    { "name": "id",          "type": { "type": "string", "logicalType": "uuid" } },
    { "name": "sagaId",      "type": { "type": "string", "logicalType": "uuid" } },
    { "name": "customerId",  "type": "string" },
    { "name": "price",       "type": { "type": "bytes", "logicalType": "decimal", "precision": 10, "scale": 2 } },
    { "name": "createdAt",   "type": { "type": "long",  "logicalType": "timestamp-millis" } },
    { "name": "paymentOrderStatus", "type": { "type": "enum", "name": "PaymentOrderStatus", "symbols": ["PENDING","CANCELLED"] } }
  ]
}
```

```java
@RequiredArgsConstructor
public class PaymentRequestKafkaPublisher {
    private final KafkaProducerHelper<String, PaymentRequestAvroModel> producer;

    public void publish(final PaymentRequestAvroModel msg) {
        producer.send("payment-request", msg.getSagaId().toString(), msg, null);
    }
}
```

## Anti-pattern

```java
// WRONG: JSON over Kafka
producer.send("payment-request", new ObjectMapper().writeValueAsString(payload));

// WRONG: untyped Map
producer.send("payment-request", Map.of("id", id, "amount", 100));

// WRONG: Avro file in the wrong module
src/main/avro/PaymentRequest.avsc                 // outside *-message-model
*-message-core/src/main/resources/avro/...        // wrong module (must be *-model)
```

## References

- Skill: `lg5-kafka-avro` (full producer/consumer recipe + schema file
  conventions + `make run-avro-model` workflow).
- Confluent Avro documentation:
  https://docs.confluent.io/platform/current/schema-registry/avro.html
- Related rules: RULE-004 (module shape), RULE-008 (outbox payload vs domain event),
  RULE-010 (Kafka listener shape).
