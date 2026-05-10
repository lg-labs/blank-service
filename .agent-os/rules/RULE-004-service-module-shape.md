---
id: RULE-004
slug: service-module-shape
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: architecture
tags: [maven-modules, project-structure, blank-service]
description: Every microservice mirrors the `blank-service` skeleton: domain (core+application-service), api, data-access, message (core+model), external (optional), container, acceptance-test, support.
---

# RULE-004 — Service module shape

## Statement

Every microservice multi-module Maven project must mirror the layout of
[`blank-service`](https://github.com/lg-labs/blank-service):

```
<svc>/
├── <svc>-domain/
│   ├── <svc>-domain-core/          # pure Java domain (aggregates, VOs, ports)
│   └── <svc>-application-service/  # Spring use-case orchestration + saga + outbox
├── <svc>-api/                      # REST adapter (@RestController, DTOs)
├── <svc>-data-access/              # JPA adapter (@Entity, repositories)
├── <svc>-message/
│   ├── <svc>-message-core/         # Kafka producer/consumer + listener helpers
│   └── <svc>-message-model/        # Avro schemas (.avsc) + generated classes
├── <svc>-external/                 # OPTIONAL: Feign clients to other systems
├── <svc>-container/                # @SpringBootApplication + application.yaml
├── <svc>-acceptance-test/          # Cucumber + Testcontainers + Wiremock ATDD
└── <svc>-support/                  # docker-compose for local infra (postgres, kafka)
```

Module names are lowercase-hyphen-separated, prefixed with the service name
(e.g. `payment-domain-core`, not `payment_domain_core`).

## Rationale

A consistent module shape lets every framework feature, every Make target,
every CI workflow, and every agent skill assume the same paths. It also
encodes the hexagonal dependency rule physically: only the inner module
(`domain-core`) is allowed to be a sink of dependencies, while adapters
(`api`, `data-access`, `message`) depend on it. The `container` module is the
single composition root, which means there is exactly one place to set up
profiles, beans, and configuration — never `@SpringBootApplication` in two
modules.

## Example — correct

```
payment/
├── payment-domain/
│   ├── payment-domain-core/
│   │   └── pom.xml                 # NO spring-* deps
│   └── payment-application-service/
│       └── pom.xml                 # depends on domain-core + lg5-spring-saga + lg5-spring-outbox
├── payment-api/
├── payment-data-access/
├── payment-message/
│   ├── payment-message-core/
│   └── payment-message-model/
├── payment-container/
│   ├── pom.xml                     # depends on ALL siblings; declares @SpringBootApplication
│   └── src/main/resources/application.yaml
├── payment-acceptance-test/
└── payment-support/
    └── docker-compose.yml
```

## Anti-pattern

- A monolithic single module containing everything (controller, entity,
  service, kafka producer in one src/).
- Two modules each with `@SpringBootApplication` (e.g. one in `*-api`, another
  in `*-container`) — the container is the **only** application entry point.
- Renaming modules to non-conventional shapes
  (`*-rest`, `*-persistence`, `*-kafka`, `*-app`) — the framework's
  archetype, the Make targets, and the agent skills all assume the conventional
  names.
- Putting Avro schemas (`.avsc`) outside `*-message-model/src/main/resources/avro/`.

## References

- Skill: `lg5-new-service` (full skeleton + how to derive new module names from `blank-service`).
- Reference repo: https://github.com/lg-labs/blank-service
- Related rules: RULE-003 (hexagonal+DDD), RULE-007 (Avro schema location).
