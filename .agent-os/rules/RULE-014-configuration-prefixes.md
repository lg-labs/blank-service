---
id: RULE-014
slug: configuration-prefixes
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: framework
tags: [configuration, properties, kafka, testcontainers, feign]
description: Use the canonical configuration property prefixes — `kafka-config.*`, `kafka-producer-config.*`, `kafka-consumer-config.*` (framework), `<svc>-service.*` (per-service), `testcontainers.<name>.enabled`, `application.image.name`, `application.traces.{console,file}.enabled` (ATDD), `third.basic.auth.{username,password}` (Feign).
---

# RULE-014 — Configuration property prefixes

## Statement

Stick to the canonical property prefixes defined by the framework. Do **not**
invent your own top-level prefixes; do **not** scatter the same setting
under multiple prefixes.

| Prefix                                               | Owner       | Use case |
|------------------------------------------------------|-------------|----------|
| `kafka-config.*`                                     | framework   | Cluster-wide Kafka settings (bootstrap servers, schema registry URL, security). |
| `kafka-producer-config.*`                            | framework   | Producer-specific tuning (acks, batch size, compression). |
| `kafka-consumer-config.*`                            | framework   | Consumer-specific tuning (`batch-listener`, group ids, max-poll-records). |
| `<svc>-service.*`                                    | per-service | All business config for the service (topic names, scheduler cadences, feature flags). E.g. `payment-service.outbox-scheduler-fixed-rate`. |
| `testcontainers.<name>.enabled`                      | ATDD        | Boolean flag that gates each `*ContainerCustomConfig` (RULE-013). |
| `application.image.name`                             | ATDD        | Docker image to use for service-under-test scenarios. |
| `application.traces.console.enabled`                 | ATDD        | Toggle console trace output. |
| `application.traces.file.enabled`                    | ATDD        | Toggle file trace output. |
| `third.basic.auth.username`                          | Feign       | Basic auth username for Feign clients to third-party systems. |
| `third.basic.auth.password`                          | Feign       | Basic auth password (use config import / env var injection). |

`<svc>-service` is **always hyphenated** (Spring relaxed binding handles
camelCase access from Java), and the suffix is the service name without the
"-service" itself appended (so `payment-service.*`, not `payment.*`).

## Rationale

Consistent prefixes are what allow:

- `@ConfigurationProperties("kafka-config")` Kotlin classes in the framework
  to bind cluster settings without per-service customization.
- The agent skills (`lg5-kafka-avro`, `lg5-outbox`, `lg5-atdd`) to give exact
  property names that are guaranteed to be picked up by the framework.
- Operators and SREs to grep-find every Kafka setting across all services
  with `kafka-`, every business feature flag with `<svc>-service.`, etc.

Inventing a prefix (`my-svc.kafka.bootstrap`) silently bypasses framework
bindings — your settings exist in `application.yaml` but never reach the
framework's `KafkaConfigData` Kotlin record, leading to puzzling defaults
being used at runtime.

## Example — correct

```yaml
# payment-container/src/main/resources/application.yaml
kafka-config:
  bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:19092}
  schema-registry-url: ${SCHEMA_REGISTRY_URL:http://localhost:8081}

kafka-producer-config:
  acks: all
  batch-size: 16384
  compression-type: snappy

kafka-consumer-config:
  batch-listener: true
  max-poll-records: 500
  payment-consumer-group-id: payment-consumer

payment-service:
  payment-request-topic-name:  payment-request
  payment-response-topic-name: payment-response
  outbox-scheduler-fixed-rate: 1000

scheduling:
  enabled: true
```

```yaml
# payment-acceptance-test/src/test/resources/application-test.yaml
testcontainers:
  postgres:        { enabled: true }
  kafka:           { enabled: true }
  schema-registry: { enabled: true }
  wiremock:        { enabled: true }

application:
  image:
    name: payment-service:test
  traces:
    console: { enabled: true }
    file:    { enabled: false }

third:
  basic:
    auth:
      username: ${THIRD_USER}
      password: ${THIRD_PASS}
```

## Anti-pattern

```yaml
# WRONG: invented prefix bypasses framework binding
my-payment-app:
  kafka:
    bootstrap: localhost:9092            # ❌ never picked up by KafkaConfigData
    topic: payment-request

# WRONG: business config under a generic 'app.*'
app:
  outbox-fixed-rate: 1000                # ❌ should be payment-service.outbox-scheduler-fixed-rate

# WRONG: dot-separated single-word top level
payment.outbox.fixedRate: 1000           # ❌ should be hyphenated payment-service.outbox-scheduler-fixed-rate
```

## References

- Skill: `lg5-spring-overview` (config property catalog).
- Skill: `lg5-kafka-avro` (Kafka prefixes in detail).
- Skill: `lg5-atdd` (testcontainers + application prefixes).
- Related rules: RULE-011 (scheduler reads `<svc>-service.*`), RULE-013
  (testcontainers gating).
