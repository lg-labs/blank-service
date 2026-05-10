---
kind: template
name: prd-template
version: 0.2.0
description: Functional PRD for an lg5-spring microservice or feature. Used by /sdd-specify. Functional only — no technology.
---

# PRD — `<feature-name>`

> **Use this template via `/sdd-specify`.** Replace every `<placeholder>`.
> The PRD is the **functional** spec: it describes the *what* and the
> *why*. It does NOT mention Spring, Kafka, REST, Postgres, Avro, or any
> implementation detail. Mark unresolved questions with
> `[NEEDS CLARIFICATION: <question>]`.

## 1. Summary

One paragraph. What does this feature/service do, for whom, and why now?

## 2. Problem

2-4 sentences. What pain exists today? What is broken or missing?

## 3. Users

- **<user role 1>** — what they want to do.
- **<user role 2>** — what they want to do.
- **<system role>** — what it needs to do (use this for service-to-service
  callers; the PRD owns business intent, not protocol).

## 4. Success metrics

| Metric | Baseline | Target | Window |
|--------|---------:|-------:|--------|
| `<metric 1>` | `<n>` | `<n>` | `<window>` |
| `<metric 2>` | `<n>` | `<n>` | `<window>` |

## 5. Requirements (in scope)

> Each requirement gets a stable ID `REQ-NNN`. Acceptance is a single
> sentence in user-observable terms. No technology.

| ID | Requirement | Acceptance |
|----|-------------|------------|
| REQ-001 | `<what the system must do>` | `<observable outcome>` |
| REQ-002 | `…` | `…` |

## 6. Out of scope

- `<thing>` — _(reason: <why excluded>)_
- `<thing>` — _(reason: <why excluded>)_

## 7. Acceptance criteria (feature-level)

- [ ] `<feature-level outcome 1>`
- [ ] `<feature-level outcome 2>`

## 8. Open questions

| Question | Decider | Due |
|---------|---------|-----|
| `<question>` | `<role>` | `<date>` |

## Definition of Done (PRD)

- [ ] Every requirement has a stable ID (REQ-NNN).
- [ ] No technology mentioned (no Spring, Kafka, Postgres, REST, …).
- [ ] Every requirement has at least one acceptance criterion.
- [ ] Pending clarifications marked with `[NEEDS CLARIFICATION: …]`.
- [ ] Out-of-scope items explicitly listed with reason.
- [ ] Stakeholder/owner identified (in the open questions table).
