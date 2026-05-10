---
id: RULE-013
slug: testcontainers-opt-in
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: testing
tags: [testcontainers, opt-in, conditional, atdd, infrastructure]
description: Each Testcontainers `*ContainerCustomConfig` is opt-in via `testcontainers.<name>.enabled`. ATDD `CucumberHooks` extending `Lg5TestBootPortNone` import them via `@Import(TestContainersLoader.class)`.
---

# RULE-013 — Testcontainers are opt-in per scenario

## Statement

Every Testcontainers configuration class (Postgres, Kafka, Schema Registry,
Wiremock, etc.) is wired as a Spring `@TestConfiguration` named
`<Name>ContainerCustomConfig` and **gated** by:

```java
@ConditionalOnProperty(value = "testcontainers.<name>.enabled", havingValue = "true")
```

The ATDD `CucumberHooks` class:

1. Extends `Lg5TestBootPortNone` (RULE-012).
2. Imports the container loader: `@Import(TestContainersLoader.class)`.
3. Sets `testcontainers.<name>.enabled` per scenario (typically in
   `application-test.yaml` or via `@DynamicPropertySource`).

The `TestContainersLoader` is a framework class (`com.lg5.spring.test.containers`)
that aggregates every available container config behind a single import; the
`enabled` flags decide which actually start.

## Rationale

Testcontainers is heavyweight: each container takes 2–10s to start, holds
ports, and uses memory. Running every container for every test would make
the suite unusable. The opt-in convention means:

- A pure JPA test only enables `testcontainers.postgres.enabled=true`.
- A pure Kafka producer test only enables `testcontainers.kafka.enabled=true`
  + `testcontainers.schema-registry.enabled=true`.
- A full saga ATDD enables all four.

It also means CI can shard scenarios and skip containers that aren't needed
for that shard, cutting the wall-clock time of the suite.

The `@Import(TestContainersLoader.class)` indirection means individual
scenarios don't need to know which container classes exist — they just flip
the flag. Adding a new container (Redis, Elasticsearch) is then a single
new gated config + a new flag, no edits to existing scenarios.

## Example — correct

```java
// payment-acceptance-test/src/test/java/.../KafkaContainerCustomConfig.java
@TestConfiguration
@ConditionalOnProperty(value = "testcontainers.kafka.enabled", havingValue = "true")
public class KafkaContainerCustomConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public KafkaContainer kafkaContainer() {
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));
    }

    @Bean
    public DynamicPropertyRegistrar kafkaProperties(final KafkaContainer kafka) {
        return registry -> registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
}
```

```java
// payment-acceptance-test/src/test/java/.../CucumberHooks.java
@CucumberContextConfiguration
@ActiveProfiles({"test", "local"})
@Import(TestContainersLoader.class)
public class CucumberHooks extends Lg5TestBootPortNone { }
```

```yaml
# payment-acceptance-test/src/test/resources/application-test.yaml
testcontainers:
  postgres:
    enabled: true
  kafka:
    enabled: true
  schema-registry:
    enabled: true
  wiremock:
    enabled: true
```

## Anti-pattern

```java
// WRONG: container always-on, no @ConditionalOnProperty.
@TestConfiguration
public class KafkaContainerCustomConfig {                  // ❌ always starts
    @Bean(initMethod = "start") public KafkaContainer kafka() { ... }
}

// WRONG: container declared inside the scenario test class
//        instead of as a reusable @TestConfiguration.
class PaymentSagaSteps {
    @Container static KafkaContainer kafka = new KafkaContainer(...);   // ❌ leaks lifecycle
}

// WRONG: importing every container class explicitly
@Import({ KafkaContainerCustomConfig.class,
          PostgresContainerCustomConfig.class,
          SchemaRegistryContainerCustomConfig.class,
          WiremockContainerCustomConfig.class })           // ❌ should be TestContainersLoader
public class CucumberHooks extends Lg5TestBootPortNone { }
```

## References

- Skill: `lg5-atdd` (full ATDD recipe with the container catalog).
- Framework class: `com.lg5.spring.test.containers.TestContainersLoader`.
- Related rules: RULE-012 (test profiles + base classes), RULE-014
  (configuration prefixes including `testcontainers.*`).
