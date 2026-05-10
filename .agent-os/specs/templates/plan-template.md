---
kind: template
name: plan-template
version: 0.1.0
description: Technical plan derived from a PRD + ADRs. Used by /sdd-plan.
---

# Plan — `<feature-name>`

> **Use this template via `/sdd-plan`.** Generated from
> [`prd.md`](prd.md) and the ADRs under [`adr/`](adr/).
> The plan describes the **how** at the architectural level; concrete
> code lives in [`tasks.md`](tasks.md) and the actual repo.

## Architecture overview

Module map (RULE-004), with one-line purpose per module.

```
<svc>-domain/
  ├── <svc>-domain-core         # <purpose>
  └── <svc>-application-service # <purpose>
<svc>-api                       # <purpose>
<svc>-data-access               # <purpose>
<svc>-message/
  ├── <svc>-message-core        # <purpose>
  └── <svc>-message-model       # <purpose>
<svc>-container                 # <purpose>
<svc>-acceptance-test           # <purpose>
<svc>-support                   # <purpose>
```

## Module ↔ requirement matrix

Every PRD requirement must be covered by ≥1 module.

| Module | Covers REQ |
|--------|------------|
| `<module>` | REQ-NNN, REQ-MMM |

## ADR index

- [ADR-001](adr/ADR-001-<slug>.md) — <one-line title>.
- [ADR-002](adr/ADR-002-<slug>.md) — <one-line title>.

## Sequenced steps

See [`tasks.md`](tasks.md) for the full TASK-NNN list. Summary of the
dependency graph:

```
TASK-001 ──┬─► TASK-002 ──► …
           └─► TASK-003 ──► …
```

## Cross-cutting concerns

- **Topics / channels:** who provisions what, when.
- **Schema registry:** compatibility mode + registration plan.
- **Observability:** dashboards, alerts, owners.
- **Security:** secrets, AuthN/Z deviations from defaults.
- **Data lifecycle:** retention, GDPR, archival.

## Risks

| ID | Risk | Mitigation | Owner |
|----|------|------------|-------|
| R1 | `<risk>` | `<mitigation>` | `<role>` |

## Estimated artifact count

- New files: `~<n>`
- Modified files: `~<n>`
- New tests: `~<n>` (unit + IT + ATDD)

## Definition of Done (Plan)

- [ ] Every PRD requirement is covered by ≥1 module in the matrix above.
- [ ] Every architectural decision is captured as an ADR under `adr/`.
- [ ] Constitutional rule violations explicitly listed and justified
      (or zero).
- [ ] Module map matches RULE-004 (service module shape).
- [ ] Open questions explicitly listed (under "Risks" or in PRD §8).
- [ ] All cross-cutting concerns assigned to a team/owner.
