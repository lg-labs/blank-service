---
name: lg5-new-service
version: 0.1.0
lg5-spring-sha: d0d754a
last-validated: 2026-05-09
description: Step-by-step recipe to scaffold a brand-new lg5-spring microservice by copying and renaming the blank-service template. Load this skill when the user asks to "create a new service", "bootstrap a microservice", "generate a μ-service", or wants to start a new bounded context on top of lg5-spring.
---

# Recipe: Create a new lg5-spring microservice

> Source template: `https://github.com/lg-labs/blank-service` (locally at `/tmp/lg5-study/blank-service/`).
> Reference implementation to imitate: `/tmp/lg5-study/food-ordering-system/order-service/`.

## Prerequisites

- JDK **21**, Maven **3.9+**, Docker, `make`.
- `lg5-spring-parent` published to local `.m2` at version `1.0.0-alpha.<sha>` (run `make publish-local` in `/tmp/lg5-study/lg5-spring`).
- The blank-service repo cloned. If missing:
  ```bash
  git clone --depth 1 https://github.com/lg-labs/blank-service.git /tmp/lg5-study/blank-service
  ```

## Decisions to confirm with the user before generating code

1. **Service name** (kebab-case, e.g. `inventory-service`).
2. **Java base package** (e.g. `com.acme.inventory`).
3. **Aggregate root name** (e.g. `Inventory`, `Reservation`).
4. **Does it consume Kafka events?** (yes → needs `<svc>-message`).
5. **Does it produce Kafka events?** (yes → needs Outbox + scheduler).
6. **Does it call third-party HTTP APIs?** (yes → needs `<svc>-external` with Feign).
7. **Persistence**: PostgreSQL via JPA (default). Other → discuss.

## Step 1 — Copy the skeleton

```bash
TARGET=/path/to/workspace/<svc>-service
cp -R /tmp/lg5-study/blank-service "$TARGET"
cd "$TARGET"
rm -rf .git
```

## Step 2 — Rename modules and packages

For each occurrence of `blank` substitute the new service name; for each occurrence of `com.blanksystem` substitute the chosen base package.

| What to rename | Where |
|---|---|
| Directories `blank-*` → `<svc>-*` | top level |
| `<artifactId>blank-*</artifactId>` | every `pom.xml` |
| `<groupId>com.blanksystem</groupId>` → new groupId | every `pom.xml` |
| Java packages `com.blanksystem.*` → `<base.pkg>.*` | every `.java` source |
| `BlankApplication` → `<Svc>Application` | `<svc>-container/src/main/java/.../BlankApplication.java` |
| `application.yaml` keys `blank-service.*` → `<svc>-service.*` | `<svc>-container/src/main/resources/application*.yaml` |

Do this with a scripted sed/find-replace pass. Verify with `grep -ri "blank" .` afterward — only the README/CHANGELOG should mention the origin.

## Step 3 — Pin the lg5-spring parent SHA

In root `pom.xml`:
```xml
<parent>
  <groupId>com.lg5.spring</groupId>
  <artifactId>lg5-spring-parent</artifactId>
  <version>1.0.0-alpha.<latest-sha></version>
  <relativePath/>
</parent>
```
Get the SHA via:
```bash
git -C /tmp/lg5-study/lg5-spring log -1 --format=%h
```

## Step 4 — Define the domain (no Spring!)

In `<svc>-domain/<svc>-domain-core/src/main/java/<base.pkg>/domain/`:

- **Aggregate root**: extend `com.labs.lg.pentagon.common.domain.entity.AggregateRoot<<Id>>`.
- **Identity**: extend `BaseId<UUID>`.
- **Value Objects**: immutable, override `equals`/`hashCode`. Use Java `record` when no behavior is needed.
- **Domain events**: implement `DomainEvent`, named `<Aggregate><Verb>edEvent` (past tense).
- **Domain services**: stateless, named `<Aggregate>DomainService` with an interface + `Impl`.
- **Domain exceptions**: `<Aggregate>DomainException extends DomainException`.

Package layout:
```
domain/
├── entity/
├── valueobject/
├── event/
├── exception/
└── service/
```

## Step 5 — Define ports in application-service

In `<svc>-domain/<svc>-application-service/src/main/java/<base.pkg>/application/`:

```
ports/
├── input/
│   ├── service/      # Use case interfaces (e.g. <Svc>ApplicationService)
│   └── message/listener/<topic>/   # Kafka response listener interfaces
└── output/
    ├── repository/   # Aggregate repository ports
    ├── message/publisher/<topic>/  # Kafka publisher ports
    └── outbox/<topic>/              # Outbox port (read/save)
```

Implement input ports in `<base.pkg>.application.<svc>service` package (e.g. `<Svc>ApplicationServiceImpl`).

## Step 6 — Adapters

| Layer | Module | Implements |
|---|---|---|
| REST in | `<svc>-api` | input port `<Svc>ApplicationService` via `@RestController` |
| JPA out | `<svc>-data-access` | output port repositories via `@Repository` adapters wrapping Spring Data |
| Kafka out | `<svc>-message/<svc>-message-core` | output port publishers via `KafkaProducer<String, *AvroModel>` |
| Kafka in | `<svc>-message/<svc>-message-core` | input port listeners via `@KafkaListener` |
| Feign out | `<svc>-external` | output port HTTP clients via `@FeignClient(configuration = FeignClientConfiguration.class)` |

Always:
- Use `final` on locals & params.
- Use records for `*Command` / `*Response` DTOs.
- Map between adapter DTOs and domain via dedicated `*DataMapper` beans.
- REST controllers `produces = "application/vnd.api.v1+json"`.

## Step 7 — Container module (the only Spring Boot app)

`<svc>-container/`:

- `pom.xml` depends on **all** sibling modules + `lg5-spring-starter` + `lg5-spring-logger` + `jib-maven-plugin`.
- `<Svc>Application.java` annotated `@SpringBootApplication`, `@EnableJpaRepositories`, `@EntityScan` pointing at `<base.pkg>.dataaccess`.
- `application.yaml` includes:
  ```yaml
  server:
    port: 8181
  spring:
    datasource:
      url: jdbc:postgresql://localhost:5432/<svc>?currentSchema=<svc>&binaryTransfer=true&reWriteBatchedInserts=true&stringtype=unspecified
      username: postgres
      password: admin
    jpa:
      hibernate.ddl-auto: validate
      open-in-view: false
  scheduling:
    enabled: true
  <svc>-service:
    outbox-scheduler-fixed-rate: 10000
    outbox-scheduler-initial-delay: 10000
    # topic names…
  kafka-config:
    bootstrap-servers: localhost:19092,localhost:29092,localhost:39092
    schema-registry-url-key: schema.registry.url
    schema-registry-url: http://localhost:8081
  kafka-producer-config: …
  kafka-consumer-config: …
  ```
- `application-test.yaml` and `application-local.yaml` for ATDD profile overrides.

## Step 8 — Wire Kafka & Outbox (only if needed)

- Add Avro schemas in `<svc>-message-model/src/main/resources/avro/<event>.avsc` with namespace `<base.pkg>.message.model.avro`.
- `make run-avro-model` regenerates classes.
- Implement Outbox per `lg5-outbox` skill.
- Implement publisher/listener per `lg5-kafka-avro` skill.
- Implement Sagas (if multi-step orchestration) per `lg5-saga` skill.

## Step 9 — Local infra

In `<svc>-support/` mirror `food-ordering-system/infrastructure/`:
- `docker-compose-kafka.yml`
- `docker-compose-postgres.yml`
- `docker-compose-schema-registry.yml`

Wire Make targets `kafka-up`, `ddbb-up`, `docker-up`, `docker-down`.

## Step 10 — ATDD bootstrap

Per `lg5-atdd` skill:
- Create `<svc>-acceptance-test/src/test/java/<base.pkg>/acceptance/boot/`:
  - `AcceptanceTestCase.java` (JUnit Platform Suite)
  - `CucumberHooks.java` extending `Lg5TestBootPortNone`, `@Import(TestContainersLoader.class)`, `@CucumberContextConfiguration`.
  - `TestContainersLoader.java` `@Import`ing the four `*ContainerCustomConfig`s plus dynamic env wiring.
- `src/test/resources/features/*.feature` (Gherkin).
- `src/test/resources/application-test.yaml` enabling required testcontainers.

## Step 11 — Build & smoke test

```bash
make install-skip-test
make docker-up
make run-app           # or run-apps if multi-service
curl -i -X POST http://localhost:8181/<resource> -H 'Content-Type: application/vnd.api.v1+json' -d '{…}'
```

## Step 12 — CI / hooks

- Copy `hooks/pre-push` from blank-service (runs `mvn clean test`).
- Optionally copy `checkstyle.xml` and the maven-checkstyle-plugin config.

## Common pitfalls

- ❌ Adding `@Component` / `@Service` in `<svc>-domain-core` — domain must stay Spring-free.
- ❌ Forgetting `@Version` on outbox JPA entities → race conditions in saga.
- ❌ Rethrowing `OptimisticLockingFailureException` from Kafka listener → infinite redelivery.
- ❌ Inventing a non-existent SHA for `lg5-spring-parent` version.
- ❌ Putting `@SpringBootApplication` outside `<svc>-container`.
- ❌ Producing Kafka payloads as JSON / POJO instead of Avro `SpecificRecordBase`.
- ❌ Skipping `produces = "application/vnd.api.v1+json"` on controllers.

## Validation checklist before declaring "done"

- [ ] `mvn clean install` green from project root.
- [ ] `mvn -pl <svc>-container spring-boot:run` starts without errors against local infra.
- [ ] `make run-acceptance-test` green (at least one happy-path feature).
- [ ] `<svc>-domain-core/pom.xml` has zero Spring dependencies.
- [ ] No `blank` / `com.blanksystem` strings remain in source.
- [ ] All controllers produce `application/vnd.api.v1+json`.
- [ ] Outbox tables include `version` column with `@Version`.
- [ ] Saga steps catch `OptimisticLockingFailureException` and short-circuit on missing outbox row.
