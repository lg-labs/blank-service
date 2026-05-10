# Constitution — lg5-spring-agent-os

> **Read this first.** These rules are **immutable** for any service that
> consumes this bundle. Every spec, plan, task, code change, and PR must
> respect them. Violations must be **explicitly justified** in an ADR or
> rejected.
>
> Concept borrowed from
> [GitHub spec-kit](https://github.com/github/spec-kit/blob/main/spec-driven.md#the-constitutional-foundation-enforcing-architectural-discipline)
> and adapted to lg5-spring.

## Scope

A rule is **constitutional** iff its `severity` is `must`. Advisory rules
(`should`, `info`) live alongside but are not part of the constitution
and may be relaxed with reviewer approval (no ADR required).

This bundle currently has **15 constitutional rules** out of 18 total.

## How to use

- **Authoring a PRD** (`/sdd-specify`): the PRD is purely functional and
  does not touch the constitution directly. Mention constitution only if
  a requirement seems incompatible with one — flag it as
  `[NEEDS CLARIFICATION]`.
- **Authoring a Plan/ADR** (`/sdd-plan`): every architectural decision is
  validated against the constitution. If the decision violates a rule,
  the ADR **must** include a "Constitutional impact" section that names
  the rule (e.g. `RULE-007`) and justifies the deviation.
- **Authoring Tasks** (`/sdd-tasks`): each TASK that touches code in the
  scope of a constitutional rule must reference the rule in its
  `references:` list.
- **Implementing a Task** (`/sdd-implement`): generated code is checked
  against the constitution by the `lg5-code-reviewer` subagent before
  the commit is finalized.

## The 15 constitutional rules

| ID | Slug | Scope | One-liner |
|----|------|-------|-----------|
| [RULE-001](RULE-001-stack-baseline.md) | stack-baseline | framework | Spring Boot 3.4.2, Spring 6.2.2, JDK 21, Kotlin 21. |
| [RULE-002](RULE-002-parent-pom.md) | parent-pom | framework | Inherit from `lg5-spring-parent` pinned to a framework SHA. |
| [RULE-003](RULE-003-hexagonal-ddd.md) | hexagonal-ddd | architecture | Domain depends on nothing Spring; ports & adapters strict. |
| [RULE-004](RULE-004-service-module-shape.md) | service-module-shape | architecture | Mirror `blank-service` module layout (8 Maven modules). |
| [RULE-005](RULE-005-no-custom-annotations.md) | no-custom-annotations | framework | Use only stock Spring + Lombok; no custom framework annotations. |
| [RULE-006](RULE-006-rest-media-type.md) | rest-media-type | architecture | REST controllers produce `application/vnd.api.v1+json`. |
| [RULE-007](RULE-007-kafka-avro-payloads.md) | kafka-avro-payloads | kafka | Kafka payloads are `SpecificRecordBase` (Avro). Key = sagaId. |
| [RULE-008](RULE-008-outbox-mandatory.md) | outbox-mandatory | outbox | Cross-boundary domain events go through Outbox; `@Version` mandatory. |
| [RULE-009](RULE-009-saga-step-idempotent.md) | saga-step-idempotent | saga | Saga steps are `@Transactional` and idempotent (outbox guard). |
| [RULE-010](RULE-010-kafka-listener-no-rethrow.md) | kafka-listener-no-rethrow | kafka | Swallow `OptimisticLockingFailureException` + not-found as NO-OP. |
| [RULE-011](RULE-011-outbox-scheduler-shape.md) | outbox-scheduler-shape | outbox | Implement `OutboxScheduler` with `@Scheduled` + `@ConditionalOnProperty`. |
| [RULE-012](RULE-012-test-profiles.md) | test-profiles | testing | IT/ATDD run with `@ActiveProfiles({"test","local"})`. |
| [RULE-013](RULE-013-testcontainers-opt-in.md) | testcontainers-opt-in | testing | Testcontainers gated by `testcontainers.<name>.enabled`. |
| [RULE-014](RULE-014-configuration-prefixes.md) | configuration-prefixes | framework | Use canonical config prefixes (`kafka-config.*`, `<svc>-service.*`, …). |
| [RULE-016](RULE-016-ddd-building-blocks-source.md) | ddd-building-blocks-source | architecture | DDD blocks come from `lg5-common-domain` (re-exporting `ddd-common-domain`). |

## The 3 advisory rules (NOT constitutional)

| ID | Slug | Severity | Note |
|----|------|----------|------|
| [RULE-015](RULE-015-code-style.md) | code-style | should | `final` locals, records for DTOs, package-by-concern. |
| [RULE-017](RULE-017-build-commands.md) | build-commands | should | Prefer Make targets over raw mvn/gradle. |
| [RULE-018](RULE-018-reference-projects.md) | reference-projects | info | Pointers to lg5-spring + food-ordering-system + blank-service. |

## Amending the constitution

Adding, removing, or changing the severity of a constitutional rule is a
**MAJOR** version bump for the bundle (e.g. `0.x.0` → `1.0.0`). Wording
clarifications without behavioral change are PATCH bumps.
