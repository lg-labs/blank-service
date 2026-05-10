---
name: lg5-atdd
version: 0.1.0
lg5-spring-sha: d0d754a
last-validated: 2026-05-09
description: How to write acceptance tests in lg5-spring services using Cucumber + JUnit Platform Suite + Testcontainers (Postgres, Confluent Kafka, Wiremock, App container). Covers Lg5TestBootPortNone, *ContainerCustomConfig opt-in, dynamic env var injection, log control. Load this skill when the user asks about ATDD, acceptance tests, end-to-end tests, Cucumber features, or testcontainers wiring.
---

# lg5-spring — Acceptance Test (ATDD) Pattern

> Framework modules: `lg5-spring-acceptance-test`, `lg5-spring-integration-test`, `lg5-spring-testcontainers`, `lg5-jvm-utils`.
> Reference impl: `/tmp/lg5-study/food-ordering-system/order-service/order-acceptance-test/`.

## What the framework gives you

| Class | Purpose |
|---|---|
| `Lg5TestBoot` | `@SpringBootTest(WebEnvironment.RANDOM_PORT)` + RestAssured + `@ActiveProfiles({"test","local"})` |
| `Lg5TestBootPortNone` | `@SpringBootTest(WebEnvironment.NONE)` + same profiles. Use this when the SUT runs as a Docker container (the Spring context only orchestrates testcontainers) |
| `PostgresContainerCustomConfig` | `@ConditionalOnProperty("testcontainers.postgres.enabled")` |
| `KafkaContainerCustomConfig` | `@ConditionalOnProperty("testcontainers.kafka.enabled")` — Confluent Kafka 7.8.1 + schema registry, ports 9092 + 9093 |
| `WiremockContainerCustomConfig` | `@ConditionalOnProperty("testcontainers.wiremock.enabled")` — Wiremock 3.11.0 |
| `AppContainerCustomConfig` | `@ConditionalOnProperty("testcontainers.app.enabled")` — runs the SUT image, accepts env vars from the others |
| `Constant.network` | Shared static Docker network |

Cucumber + JUnit Platform Suite + Cucumber Spring are exposed transitively from `lg5-spring-acceptance-test`.

## Module dependencies (`<svc>-acceptance-test/pom.xml`)

```xml
<dependencies>
  <dependency>
    <groupId>com.lg5.spring</groupId>
    <artifactId>lg5-spring-acceptance-test</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>com.lg5.spring</groupId>
    <artifactId>lg5-spring-testcontainers</artifactId>
    <scope>test</scope>
  </dependency>
  <!-- and the SUT's container module so Spring can scan beans -->
  <dependency>
    <groupId>${project.groupId}</groupId>
    <artifactId><svc>-container</artifactId>
  </dependency>
</dependencies>
```

## File layout

```
<svc>-acceptance-test/
├── src/test/java/<base.pkg>/acceptance/
│   ├── boot/
│   │   ├── AcceptanceTestCase.java         # JUnit Platform Suite entry
│   │   ├── CucumberHooks.java              # Spring context bootstrap
│   │   └── TestContainersLoader.java       # Aggregates the *CustomConfig beans
│   └── steps/
│       ├── <Feature>Steps.java             # @Given / @When / @Then
│       └── CommonSteps.java                # Shared steps
├── src/test/resources/
│   ├── features/<feature>.feature
│   ├── application-test.yaml               # toggles testcontainers.*.enabled
│   └── wiremock/<scenario>.json            # optional Wiremock stubs
└── pom.xml
```

## 1) JUnit Platform Suite

```java
package com.acme.svc.acceptance.boot;

import org.junit.platform.suite.api.*;
import io.cucumber.junit.platform.engine.Constants;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameters({
    @ConfigurationParameter(
        key   = Constants.PLUGIN_PROPERTY_NAME,
        value = "pretty, json:target/atdd-reports/cucumber.json, html:target/atdd-reports/cucumber-reports.html"),
    @ConfigurationParameter(
        key   = Constants.GLUE_PROPERTY_NAME,
        value = "com.acme.svc.acceptance"),
    @ConfigurationParameter(
        key   = Constants.FILTER_TAGS_PROPERTY_NAME,
        value = "${cucumber.filter.tags:not @ignore}")
})
class AcceptanceTestCase { }
```

The `${cucumber.filter.tags:…}` placeholder enables `make run-at-by-tag TAG_NAME=@smoke` from the parent Make.

## 2) Spring + Cucumber bootstrap

```java
package com.acme.svc.acceptance.boot;

import com.lg5.spring.integration.test.boot.Lg5TestBootPortNone;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.context.annotation.Import;

@Import(TestContainersLoader.class)
@CucumberContextConfiguration
public final class CucumberHooks extends Lg5TestBootPortNone { }
```

`Lg5TestBootPortNone` already applies:
- `@SpringBootTest(webEnvironment = WebEnvironment.NONE)`
- `@ActiveProfiles({"test","local"})`
- `@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)`

## 3) TestContainersLoader

```java
package com.acme.svc.acceptance.boot;

import com.lg5.spring.testcontainers.*;
import org.springframework.context.annotation.*;

@Configuration
@Import({
    PostgresContainerCustomConfig.class,
    KafkaContainerCustomConfig.class,
    WiremockContainerCustomConfig.class,
    AppContainerCustomConfig.class
})
public class TestContainersLoader {
    // Containers are started when the corresponding testcontainers.<name>.enabled = true.
    // Each *ContainerCustomConfig publishes a Map<String,String> of env vars (via
    // initManualConnectionPropertiesMap) that AppContainerCustomConfig consumes.
}
```

## 4) application-test.yaml

```yaml
testcontainers:
  postgres:
    enabled: true
  kafka:
    enabled: true
  wiremock:
    enabled: true
  app:
    enabled: true

application:
  image:
    name: ${IMAGE_NAME:com.acme/<svc>-service:latest}     # ← built by jib in <svc>-container
  server:
    port: 8181
  traces:
    console:
      enabled: true
    file:
      enabled: ${FILE_LOG:false}
  log:
    source:
      path: /logs
    destination:
      path: ./target/logs/<svc>-service
```

The `application.traces.{console,file}.enabled` flags are LG-71 additions for routing the SUT container's stdout/log files into the host.

## 5) Feature file (Gherkin)

```gherkin
# src/test/resources/features/order_payment_happy_path.feature
@order @smoke
Feature: An order is paid successfully

  Background:
    Given the order service is running
    And the payment service stub returns a successful payment

  Scenario: Customer creates an order and the payment is approved
    When a customer submits a valid order with total 25.50
    Then the order is persisted with status PENDING
    And a payment-request event is published with the same sagaId
    And after the payment-response event, the order status becomes PAID
```

## 6) Steps

```java
package com.acme.svc.acceptance.steps;

import io.cucumber.java.en.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import static org.assertj.core.api.Assertions.*;

public class OrderPaymentSteps {

    @Value("${application.server.port}") private int sutPort;
    @Autowired private KafkaTemplate<String, PaymentResponseAvroModel> kafkaTemplate;
    @Autowired private OrderJpaRepository repo;

    private Response response;
    private String trackingId;

    @Given("the order service is running")
    public void theOrderServiceIsRunning() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port    = sutPort;
    }

    @When("a customer submits a valid order with total {double}")
    public void aCustomerSubmitsAValidOrder(final double total) {
        response = RestAssured.given()
            .contentType("application/vnd.api.v1+json")
            .body(buildCreateOrderJson(total))
            .post("/orders");
        trackingId = response.jsonPath().getString("trackingId");
    }

    @Then("the order is persisted with status PENDING")
    public void theOrderIsPersistedAsPending() {
        await().untilAsserted(() ->
            assertThat(repo.findByTrackingId(UUID.fromString(trackingId)))
                .hasValueSatisfying(o -> assertThat(o.getOrderStatus()).isEqualTo("PENDING")));
    }

    @And("after the payment-response event, the order status becomes PAID")
    public void afterPaymentResponseOrderIsPaid() {
        kafkaTemplate.send("payment-response", buildPaymentResponseAvro(trackingId, "COMPLETED"));
        await().untilAsserted(() ->
            assertThat(repo.findByTrackingId(UUID.fromString(trackingId)))
                .hasValueSatisfying(o -> assertThat(o.getOrderStatus()).isEqualTo("PAID")));
    }
}
```

Use `org.awaitility.Awaitility.await()` for asynchronous assertions (Kafka roundtrip + outbox scheduler).

## 7) Wiremock stubs (HTTP third parties)

If your SUT calls external HTTP, `WiremockContainerCustomConfig` exposes a wiremock instance reachable at the network alias `wiremock`. Stub via:

```json
// src/test/resources/wiremock/payment-stub.json
{
  "request":  { "method": "POST", "url": "/payments" },
  "response": { "status": 200, "headers": { "Content-Type": "application/json" },
                "body": "{\"status\":\"APPROVED\"}" }
}
```

Loaded automatically if mounted into the wiremock container; or programmatically via `WireMock.givenThat(...)` against `wiremock.getMappedPort(8080)`.

## 8) Make targets

```bash
make run-acceptance-test                 # build + failsafe **/*IT.java
make run-acceptance-test-alone           # only failsafe **/*AcceptanceT*.java
make run-at-by-tag TAG_NAME=@smoke       # filter by Cucumber tag
make run-test-spec TEST_NAME=OrderIT     # single spec
make run-atdd-module ATDD=order-service/order-acceptance-test FILE_LOG=true
```

## 9) Image build prerequisite

`testcontainers.app.enabled=true` requires the SUT image to exist locally:

```bash
mvn -pl <svc>-container -am package jib:dockerBuild
# or use the Make target that wraps it
```

## 10) Disabling scheduling in ATDD (when not needed)

When you want full deterministic control of the outbox, disable the scheduler:

```yaml
# application-test.yaml
scheduling:
  enabled: false
```

Then trigger `outboxScheduler.processOutboxMessage()` manually from a step, or assert directly on outbox rows.

> In food-ordering-system this is done **exclusively via YAML profile**. There is no `@TestPropertySource("scheduling.enabled=false")` anywhere in the repo. The scheduler beans use `@ConditionalOnProperty(value = "scheduling.enabled", havingValue = "true", matchIfMissing = true)`, so simply omitting the YAML override leaves them **enabled** in any test that doesn't activate the `test` profile. ATDD must always activate `test` (which `Lg5TestBootPortNone` does via `@ActiveProfiles({"test","local"})`) and ship an `application-test.yaml` with `scheduling.enabled: false` if it wants determinism.

## 11) `@Sql` setup/cleanup integration tests

For non-Cucumber container ITs (e.g. testing a saga step in isolation), the canonical pattern in food-ordering-system is `@Sql` setup + cleanup in `*-container/src/test/`:

```java
// order-container/src/test/java/.../outbox/OrderPaymentSagaIT.java
@Sql(value = {"classpath:sql/OrderPaymentSagaTestSetUp.sql"})
@Sql(value = {"classpath:sql/OrderPaymentSagaTestCleanUp.sql"},
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class OrderPaymentSagaIT extends Bootstrap {
    @Autowired private OrderPaymentSaga orderPaymentSaga;

    @Test
    void it_should_completed_payment_but_try_again_the_payment_should_is_already_processed() {
        orderPaymentSaga.process(getPaymentResponse());
        orderPaymentSaga.process(getPaymentResponse());   // second call must be a no-op
    }
}
```

Layout:
- IT class: `<svc>-container/src/test/java/.../<feature>IT.java`
- SQL files: `<svc>-container/src/test/resources/sql/*.sql`
- Base class `Bootstrap` extends `Lg5TestBoot` (or `Lg5TestBootPortNone`) and `@Import`s the testcontainers config.

Use this pattern for **deterministic single-flow tests** that need a real DB but not a full Cucumber harness. ATDD with Cucumber is for **end-to-end** scenarios that include Kafka roundtrips and the SUT running as a Docker container.

## Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Use Thread.sleep | Use `Awaitility.await().untilAsserted(...)` |
| Hard-code container ports | Read from `application.yaml` properties or `*ContainerCustomConfig` beans |
| Start containers in `@BeforeAll` manually | Let `TestContainersLoader` + `@ConditionalOnProperty` do it |
| Run ATDD with `scheduling.enabled=true` AND assert exact timings | Either disable scheduling or use generous `Awaitility` polling |
| Reuse `Lg5TestBoot` (RANDOM_PORT) when SUT runs as image | Use `Lg5TestBootPortNone` |
| Forget to build the SUT image | `mvn jib:dockerBuild` before `make run-acceptance-test` |
| Mix Cucumber glue with non-acceptance tests | Keep glue package narrow via `Constants.GLUE_PROPERTY_NAME` |

## Where to look in the reference repo

| Question | File |
|---|---|
| How is the suite declared? | `food-ordering-system/order-service/order-acceptance-test/src/test/java/.../boot/AcceptanceTestCase.java` |
| How is Cucumber wired to Spring? | `…/boot/CucumberHooks.java` |
| How are containers loaded conditionally? | `…/boot/TestContainersLoader.java` |
| Which env vars get pushed to the SUT? | `lg5-spring-testcontainers/.../*ContainerCustomConfig.initManualConnectionPropertiesMap` |
| Where to override defaults per scenario? | `src/test/resources/application-test.yaml` |
