---
name: lg5-spring-overview
version: 0.1.0
lg5-spring-sha: d0d754a
last-validated: 2026-05-09
description: Overview, module map, recent commit insights, and global conventions of the lg5-spring framework (https://github.com/lg-labs-pentagon/lg5-spring). Load this skill when the user asks "what is lg5-spring", "what modules does it have", "what changed recently", or needs a high-level orientation before diving into a specific topic.
---

# lg5-spring — Overview & Module Map

> Stack baseline (LG-77, Feb 2025): Spring Boot **3.4.2**, Spring Framework **6.2.2**, JDK **21**, Kotlin **21**.

## What it is

`lg5-spring` is a **Maven BOM + Gradle-published libraries** that standardize μ-service development around:

- **Hexagonal architecture** (ports & adapters)
- **DDD** (re-exports `com.labs.lg.pentagon:ddd-common-domain`)
- **SAGA** orchestration (`SagaStep<T>` interface)
- **Transactional Outbox** (`OutboxStatus`, `OutboxScheduler`)
- **Kafka + Avro** (typed producers/consumers over `SpecificRecordBase`)
- **Testcontainers + Cucumber ATDD** with opt-in containers

It does **not** ship custom annotations. Conventions rely on stock Spring + Lombok + a strict module/package layout.

## Three repositories under study

| Repo | Role | Build tool | URL |
|---|---|---|---|
| `lg5-spring/` | The framework — Gradle Kotlin DSL → Maven BOM | Gradle | https://github.com/lg-labs-pentagon/lg5-spring |
| `food-ordering-system/` | Reference implementation: 4 μ-services with full Saga | Maven | https://github.com/lg-labs/food-ordering-system |
| `blank-service/` | Empty skeleton; copy & rename for new services | Maven | https://github.com/lg-labs/blank-service |

Local clones expected at `/tmp/lg5-study/{lg5-spring, food-ordering-system, blank-service}`. If missing:
```bash
mkdir -p /tmp/lg5-study
git clone --depth 50 https://github.com/lg-labs-pentagon/lg5-spring.git /tmp/lg5-study/lg5-spring
git clone --depth 1  https://github.com/lg-labs/food-ordering-system.git /tmp/lg5-study/food-ordering-system
git clone --depth 1  https://github.com/lg-labs/blank-service.git        /tmp/lg5-study/blank-service
```

## Framework module map

```
lg5-spring/
├── lg5-spring-parent              # Maven BOM (Gradle Kotlin publication)
├── lg5-spring-starter             # Aggregator: pulls Spring Boot starters
├── lg5-spring-api-rest            # GlobalExceptionHandler + ErrorDTO
├── lg5-spring-client              # Feign + Basic-auth + CustomErrorDecoder
├── lg5-spring-data-jpa            # JPA aggregator
├── lg5-spring-logger              # MDC seeding (BaseMDC + Lg5SpringVersion)
├── lg5-spring-utils               # Misc utilities
├── lg5-spring-kafka/
│   ├── lg5-spring-kafka-config    # Kotlin @ConfigurationProperties
│   ├── lg5-spring-kafka-consumer  # Generic ConsumerFactory + KafkaConsumer iface
│   ├── lg5-spring-kafka-producer  # Generic ProducerFactory + KafkaProducer + helper
│   └── lg5-spring-kafka-model     # Avro plugin home
├── lg5-spring-outbox              # Kotlin: OutboxStatus + OutboxScheduler iface
├── lg5-spring-testcontainers      # Postgres / Confluent-Kafka / Wiremock / App containers
├── lg5-spring-integration-test    # @SpringBootTest base classes
├── lg5-spring-acceptance-test     # JUnit Platform Suite + Cucumber wiring
├── lg5-common/
│   ├── lg5-common-domain          # Re-exports external ddd-common-domain
│   └── lg5-common-application-service
├── lg5-jvm-saga                   # Kotlin: SagaStep<T> (process / rollback)
├── lg5-jvm-utils                  # Pure JVM utilities
└── lg5-jvm-unit-test              # Pure JVM unit-test helpers
```

## Public abstractions cheatsheet

| Symbol | Package | Notes |
|---|---|---|
| `SagaStep<T>` | `com.lg5.spring.saga` | `process(T)` + `rollback(T)` (Kotlin interface) |
| `OutboxStatus` | `com.lg5.spring.outbox` | enum: `STARTED \| COMPLETED \| FAILED` |
| `OutboxScheduler` | `com.lg5.spring.outbox` | single method `processOutboxMessage()` |
| `KafkaProducer<K,V>` | `com.lg5.spring.kafka.producer.service` | `V extends SpecificRecordBase` |
| `KafkaProducerImpl<K,V>` | `com.lg5.spring.kafka.producer.service` | Wraps `KafkaTemplate` |
| `KafkaMessageHelper` | `com.lg5.spring.kafka.producer` | `getKafkaCallback(...)`, `stringToObjectClass(...)` |
| `KafkaConsumer<T>` / `KafkaConsumerV2<T>` | `com.lg5.spring.kafka.consumer` | V2 is Kotlin batch variant |
| `GlobalExceptionHandler` + `ErrorDTO` | `com.lg5.spring.api.rest` | 500 generic / 400 validation |
| `BaseMDC` | `com.lg5.spring.mdc` | Auto-puts spring/Java/Boot/Lg5 versions in MDC |
| `Lg5TestBoot` | `com.lg5.spring.integration.test.boot` | `@SpringBootTest(RANDOM_PORT)` + RestAssured |
| `Lg5TestBootPortNone` | `com.lg5.spring.integration.test.boot` | `@SpringBootTest(NONE)` |
| `PostgresContainerCustomConfig` | `com.lg5.spring.testcontainers` | `testcontainers.postgres.enabled` |
| `KafkaContainerCustomConfig` | `com.lg5.spring.testcontainers` | `testcontainers.kafka.enabled` |
| `WiremockContainerCustomConfig` | `com.lg5.spring.testcontainers` | `testcontainers.wiremock.enabled` |
| `AppContainerCustomConfig` | `com.lg5.spring.testcontainers` | `testcontainers.app.enabled`, runs SUT as Docker |
| `Constant.network` | `com.lg5.spring.testcontainers` | Shared static Docker network |

## Service (consumer) module shape

Mirror `blank-service/` 1:1:

```
<svc>-domain/
  ├─ <svc>-domain-core             # Pure DDD: aggregates, VOs, domain events, domain services. NO Spring.
  └─ <svc>-application-service     # Use cases, ports/{input,output}, sagas, outbox helpers/schedulers
<svc>-api                          # @RestController adapters, @ControllerAdvice
<svc>-data-access                  # JPA entities, Spring Data repos, output port adapters
<svc>-message/
  ├─ <svc>-message-core            # @KafkaListener + Publisher implementations
  └─ <svc>-message-model           # .avsc → generated Avro classes
<svc>-external                     # Optional: Feign clients
<svc>-container                    # @SpringBootApplication + application.yaml + jib
<svc>-acceptance-test              # Cucumber + Testcontainers (Lg5TestBootPortNone)
<svc>-support                      # docker-compose for local infra
```

## Recent commit insights (relevant to AI context)

### LG-77 (Oct 2025 docs · Feb 2025 code)
- Upgrade to **Spring Boot 3.4.2** + Docker images.
- New docs published.

### LG-71 (43 commits, Oct 2024) — Testcontainers / Kafka overhaul
- Every `*ContainerCustomConfig` gated by `@ConditionalOnProperty(testcontainers.<name>.enabled)`.
- New `AppContainerCustomConfig` boots SUT as Docker image (`application.image.name`).
- `initManualConnectionPropertiesMap` exposes env vars (`SPRING_DATASOURCE_URL`, `KAFKA-CONFIG_BOOTSTRAP-SERVERS`, etc.) to push into the SUT container.
- Schema-registry container added; Kafka exposed on 9092 + 9093.
- Avro plugin honors `testSourceDirectory` (`src/test/avro` also generated).
- Log control via `application.traces.{console,file}.enabled`.
- Confluent images bumped: `confluentinc/cp-kafka:7.8.1`, `wiremock/wiremock:3.11.0`.

### LG-70 / LG-69 / LG-45 — Test library split
- `lg5-jvm-test` (deleted) → split into `lg5-jvm-unit-test` (pure JVM) + `lg5-spring-integration-test` (Spring) + `lg5-spring-acceptance-test` (Cucumber).
- ATDD module now `api`s JUnit 5, JUnit Platform Suite, Cucumber Java/JUnit/Spring.

## Versioning convention

Framework artifacts are versioned `1.0.0-alpha.<short-git-sha>` (e.g. `1.0.0-alpha.d0d754a`). Consumers pin the SHA explicitly. To bump: pick a newer SHA from `git -C /tmp/lg5-study/lg5-spring log --oneline -20` and run `make publish-local` in the framework repo.

## Configuration property prefixes

| Prefix | Defined in | Purpose |
|---|---|---|
| `kafka-config.*` | `lg5-spring-kafka-config` | bootstrap-servers, schema-registry-url, num-of-partitions, replication-factor |
| `kafka-producer-config.*` | `lg5-spring-kafka-config` | serializers, acks, batch-size, retry-count |
| `kafka-consumer-config.*` | `lg5-spring-kafka-config` | deserializers, group-id, batch-listener, concurrency-level |
| `<svc>-service.*` | per-service | business config (topic names, scheduler rates) |
| `scheduling.enabled` | per-service | gates `@Scheduled` outbox relays |
| `testcontainers.<name>.enabled` | `lg5-spring-testcontainers` | per-container opt-in |
| `application.image.name` | `lg5-spring-testcontainers` | Docker image to run as SUT in ATDD |
| `application.traces.{console,file}.enabled` | `lg5-spring-testcontainers` | ATDD log control |
| `third.basic.auth.{username,password}` | `lg5-spring-client` | Feign basic auth |

## Build & dev workflow

### Framework
```bash
cd /tmp/lg5-study/lg5-spring
make all-build         # ./gradlew build
make publish-local     # publishes 1.0.0-alpha.<sha> to ~/.m2
```

### Service (food-ordering-system / blank-service)
```bash
make install-skip-test           # mvn clean install -DskipTests
make run-avro-model              # regen Avro classes after editing .avsc
make docker-up                   # local infra (kafka, postgres, schema-registry, wiremock)
make run-apps                    # run all services in parallel
make run-acceptance-test         # Cucumber + Testcontainers
make run-at-by-tag TAG_NAME=@smoke
make run-test-spec TEST_NAME=OrderPaymentSagaIT
```

## When to load other skills

| Task | Skill |
|---|---|
| Create a new μ-service from scratch | `lg5-new-service` |
| Implement / modify a Saga step | `lg5-saga` |
| Add the Outbox pattern + scheduler | `lg5-outbox` |
| Wire Kafka producer/consumer + Avro | `lg5-kafka-avro` |
| Write or fix acceptance tests | `lg5-atdd` |

## Sources of truth

- `/tmp/lg5-study/lg5-spring/gradle/libs.versions.toml` — single source for all dependency versions.
- `/tmp/lg5-study/lg5-spring/lg5-spring-parent/build.gradle.kts` — BOM publication.
- `/tmp/lg5-study/food-ordering-system/order-service/` — most complete reference implementation.
- `/tmp/lg5-study/blank-service/` — module-shape canonical template.
