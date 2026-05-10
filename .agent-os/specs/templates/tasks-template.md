---
kind: template
name: tasks-template
version: 0.1.0
description: Atomic, verifiable TASK-NNN list derived from a Plan. Used by /sdd-tasks; consumed by /sdd-implement.
---

# Tasks — `<feature-name>`

> **Use this template via `/sdd-tasks`.** Generated from
> [`plan.md`](plan.md). Each task is atomic (≤1 day, 1-3 commits),
> references its source REQ-NNN, and has Given/When/Then acceptance
> criteria.
>
> `/sdd-implement <task-id>` consumes one task at a time and updates its
> `Status` field upon successful commit.

## TASK-001 — `<short imperative title>`

- **Status:** `todo` | `in_progress` | `done` | `blocked`
- **References:** REQ-NNN, RULE-XXX, ADR-NNN
- **Depends on:** TASK-NNN, TASK-MMM | —
- **Modules touched:** `<module>`, `<module>`
- **Skill:** `<skill-name>` (see `.agent-os/skills/`)
- **Command / Subagent:** `/<command> <args>` | `<subagent-name>` | (none)
- **Acceptance:**
  - **Given** `<precondition>`
  - **When** `<action>`
  - **Then** `<observable outcome>`

## TASK-002 — `<short imperative title>`

- **Status:** todo
- **References:** REQ-NNN, RULE-XXX
- **Depends on:** TASK-001
- **Modules touched:** `<module>`
- **Skill:** `<skill>`
- **Command:** `/<command>`
- **Acceptance:**
  - **Given** …
  - **When** …
  - **Then** …

<!-- Add as many TASK-NNN sections as the plan requires. -->

## Definition of Done (Tasks)

- [ ] Every TASK references ≥1 REQ-NNN.
- [ ] Every TASK has Given/When/Then acceptance criteria.
- [ ] Every TASK is ≤1 day of work / 1-3 commits.
- [ ] Dependencies form a DAG (no cycles) — verify against `plan.md`.
- [ ] First TASK is the project skeleton (or smallest precondition).
- [ ] Last TASK is "all ATDD scenarios green + zero `must` violations".
- [ ] Each TASK names the exact module(s), skill(s), and command(s)/
      subagent(s) it uses.
