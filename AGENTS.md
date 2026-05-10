# AGENTS.md — lg5-spring-agent-os (upstream template)

This file is the **upstream template** shipped by `lg5-spring-agent-os`.
Consumer repositories that install this bundle should copy or merge it
into their own root-level `AGENTS.md`.

> **Path convention in this repo.** Artifacts are installed at `.agent-os/`
> (rules, skills, commands, subagents, specs). The upstream bundle is pinned
> as a git submodule at `.lg5-agent-os/` (currently `v0.3.2`). All relative
> links below have been rewritten to point at `.agent-os/...`.

---

## Spec-Driven Development workflow (read this if doing a feature)

This bundle implements the **spec-anchored** variant of SDD described by
[Fowler & Böckeler](https://martinfowler.com/articles/exploring-gen-ai/sdd-3-tools.html),
borrowing structural ideas from
[GitHub spec-kit](https://github.com/github/spec-kit).

```
   /sdd-specify     /sdd-plan         /sdd-tasks        /sdd-implement
       │                │                  │                  │
       ▼                ▼                  ▼                  ▼
     prd.md   ──►  plan.md + adr/  ──►  tasks.md   ──►   code + tests
                  + data-model.md       (TASK-NNN)        + commit
   (functional)   (technical)           (atomic)         (per task, loop)
       │                │                  │                  │
       └─ HUMAN ────────┴────── HUMAN ─────┴── HUMAN ────────►
          APPROVES        APPROVES          APPROVES
```

Per-feature artifacts live under `docs/specs/<NNN-slug>/` in the consumer
repo. See [`specs/README.md`](.agent-os/specs/README.md) for the full layout and
[`specs/examples/loyalty-ledger/`](.agent-os/specs/examples/loyalty-ledger/) for an
end-to-end example.

---

## Constitution (15 immutable rules)

The 15 rules with `severity: must` form the **constitution**. They are
immutable and bind every PRD/Plan/Task/code change. See
[`rules/CONSTITUTION.md`](.agent-os/rules/CONSTITUTION.md) for the full index +
rules of engagement, and [`rules/RULE-NNN-*.md`](.agent-os/rules/) for each rule's
statement, rationale, examples, and anti-patterns.

| ID         | Const? | Scope         | One-liner                                                                  |
|------------|:------:|---------------|----------------------------------------------------------------------------|
| RULE-001   | ✅ | framework     | Stack baseline: Spring Boot 3.4.2, Spring 6.2.2, JDK 21, Kotlin 21, Gradle/Maven. |
| RULE-002   | ✅ | framework     | Parent POM `com.lg5.spring:lg5-spring-parent:1.0.0-alpha.<short-git-sha>`. |
| RULE-003   | ✅ | architecture  | Hexagonal + DDD; domain core is Spring-free.                               |
| RULE-004   | ✅ | architecture  | Mirror the `blank-service` module shape (8 modules).                       |
| RULE-005   | ✅ | framework     | No custom framework annotations — stock Spring + Lombok only.              |
| RULE-006   | ✅ | architecture  | REST controllers produce `application/vnd.api.v1+json`.                    |
| RULE-007   | ✅ | kafka         | Kafka payloads must be Avro (`SpecificRecordBase`); schemas in `*-message-model`. |
| RULE-008   | ✅ | outbox        | Transactional Outbox is mandatory; entity must have `@Version` + `OutboxStatus`. |
| RULE-009   | ✅ | saga          | `SagaStep<T>` `@Component`; `process`/`rollback` `@Transactional` + idempotent. |
| RULE-010   | ✅ | kafka         | Kafka listeners batch by default; swallow `OptimisticLock` + not-found as NO-OP. |
| RULE-011   | ✅ | outbox        | Outbox scheduler implements `OutboxScheduler`, gated by `scheduling.enabled`. |
| RULE-012   | ✅ | testing       | IT/ATDD: `@ActiveProfiles({"test","local"})` + extend `Lg5TestBoot[PortNone]`. |
| RULE-013   | ✅ | testing       | Testcontainers opt-in via `testcontainers.<name>.enabled`.                 |
| RULE-014   | ✅ | framework     | Use canonical config prefixes (`kafka-config.*`, `<svc>-service.*`, …).    |
| RULE-015   | ⚠ | style         | `final` everywhere, records for DTOs, Kotlin only for interfaces/config.   |
| RULE-016   | ✅ | architecture  | DDD blocks come from `ddd-common-domain` (re-exported by `lg5-common-domain`). |
| RULE-017   | ⚠ | build         | Prefer Make targets (`make all-build`, `make run-apps`, `make run-acceptance-test`). |
| RULE-018   | ⚠ | reference     | Ground answers against `lg5-spring`, `food-ordering-system`, `blank-service` cloned in `/tmp/lg5-study/`. |

Legend: ✅ constitutional (`severity: must`) · ⚠ advisory (`should`/`info`).

---

## Skill routing table (load on demand)

When the user asks anything related to lg5-spring, **load the relevant skill**:

| Topic                                                       | Skill                    |
|-------------------------------------------------------------|--------------------------|
| Overview, module map, recent changes, conventions           | `lg5-spring-overview`    |
| Scaffolding a brand-new microservice from `blank-service`   | `lg5-new-service`        |
| Implementing a `SagaStep` orchestration                     | `lg5-saga`               |
| Implementing the Transactional Outbox + scheduler           | `lg5-outbox`             |
| Kafka producer/consumer + Avro schemas                      | `lg5-kafka-avro`         |
| Acceptance tests (Cucumber + Testcontainers + Wiremock)     | `lg5-atdd`               |
| Real-world patterns from food-ordering-system               | `food-ordering-system`   |

---

## Command catalog

Two categories: **SDD orchestrators** drive the workflow phases;
**building blocks** are invoked from inside `/sdd-implement` to actually
generate code.

### SDD orchestrators

| Command                          | What it does                                                       |
|----------------------------------|--------------------------------------------------------------------|
| `/sdd-specify <slug> "<desc>"`   | Convert informal prompt → functional PRD (no technology).          |
| `/sdd-plan <NNN-slug>`           | Generate `plan.md` + ADRs (+ `data-model.md`) from approved PRD.   |
| `/sdd-tasks <NNN-slug>`          | Decompose Plan into atomic `TASK-NNN` with Given/When/Then AC.     |
| `/sdd-implement <TASK-NNN>`      | Execute ONE task (code + tests + commit). Loops by re-invocation.  |

### Building blocks (called from inside /sdd-implement)

| Command                | What it does                                                          |
|------------------------|-----------------------------------------------------------------------|
| `/scaffold-service`    | Scaffolds a new microservice from `blank-service` skeleton.           |
| `/add-saga`            | Adds a `SagaStep` end-to-end (publisher + listener + outbox + scheduler). |
| `/add-outbox`          | Adds an outbox (entity + DDL + helper + scheduler) for one event type. |
| `/add-kafka-listener`  | Adds a Kafka listener (batch + NO-OP exception handling per RULE-010). |

See `commands/<name>.md` for each command's full prompt and parameters.

---

## Subagent catalog

| Subagent              | Purpose                                                          |
|-----------------------|------------------------------------------------------------------|
| `lg5-code-reviewer`   | Reviews diffs against the 18 rules; cites violations by RULE-ID. |
| `lg5-test-generator`  | Generates IT/ATDD test scaffolds (RULE-012/013 patterns).        |
| `lg5-planner`         | Decomposes feature → rule-aligned implementation plan.           |

---

## Spec templates

Under [`specs/templates/`](specs/templates/):

| Template                   | Used by             | Purpose                                                  |
|----------------------------|---------------------|----------------------------------------------------------|
| `prd-template`             | `/sdd-specify`      | Functional PRD (REQ-NNN with AC; tech-free).             |
| `plan-template`            | `/sdd-plan`         | Module map, ADR index, dep graph, risks.                 |
| `adr-template`             | `/sdd-plan`         | Lightweight ADR with constitutional impact section.      |
| `data-model-template`      | `/sdd-plan`         | Aggregates, events, outbox, REST DTOs, Avro, JPA.        |
| `tasks-template`           | `/sdd-tasks`        | Atomic TASK-NNN with Given/When/Then AC + DoD checklist. |
| `research-template`        | (manual)            | Optional time-boxed spike doc.                           |

End-to-end example: [`specs/examples/loyalty-ledger/`](.agent-os/specs/examples/loyalty-ledger/).

---

## When uncertain

- Cite the canonical source: framework path inside
  `/tmp/lg5-study/lg5-spring/...` or the real example under
  `/tmp/lg5-study/food-ordering-system/...` (RULE-018).
- Prefer copying patterns from `food-ordering-system/order-service` (the
  most complete example: REST + JPA + Kafka producer/consumer + Saga +
  Outbox + ATDD).
- Never invent framework classes. If a class isn't in the skill files,
  the rules, or the cloned repos, say so explicitly (RULE-005, RULE-018).
- Never override a constitutional rule (`severity: must`) without a
  dedicated ADR justifying the override and time-boxing the deviation.
