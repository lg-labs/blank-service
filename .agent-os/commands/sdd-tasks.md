---
description: SDD Tasks phase. Decompose an approved Plan into atomic TASK-NNN with Given/When/Then acceptance criteria under docs/specs/<NNN-slug>/tasks.md.
argument-hint: <NNN-feature-slug>
allowed-tools: bash, read, write, edit, glob, grep
---

# /sdd-tasks

You are running the **Tasks** phase of Spec-Driven Development.

> Read first: `docs/specs/<NNN-slug>/{prd,plan}.md` (must exist with
> their DoD checklists ticked) and any `adr/ADR-*.md`. Then read
> `.agent-os/specs/templates/tasks-template.md`.

## Inputs

- `<NNN-feature-slug>` — folder name under `docs/specs/`. If missing, ask.

## Pre-flight

1. Verify `prd.md` and `plan.md` exist and their DoD checklists are
   fully ticked. If not, STOP and report.
2. Read every ADR under `adr/` so you can cross-reference them in tasks.

## Steps

1. **Copy the template**:
   ```
   cp .agent-os/specs/templates/tasks-template.md \
      docs/specs/<NNN-slug>/tasks.md
   ```

2. **Decompose the Plan into atomic TASKs**. Rules of decomposition:
   - Each TASK is **≤1 day of work**, **1-3 commits**.
   - Each TASK touches **1-2 modules** maximum (cross-module work is
     usually a sign the TASK is too big — split it).
   - Each TASK has **Given/When/Then** acceptance criteria that the
     `lg5-test-generator` subagent could turn into an automated test.
   - The **first TASK** is always the project skeleton (or the smallest
     possible precondition).
   - The **last TASK** is always: "all ATDD scenarios green + zero
     `must` violations from `lg5-code-reviewer`".
   - Dependencies form a **DAG** (no cycles). Verify by drawing it
     mentally; print the dep list in a comment block at the top of
     `tasks.md`.

3. **Reference everything** for each TASK:
   - REQ-NNN (≥1) from the PRD.
   - RULE-NNN (≥0) from the constitution if the work touches that rule's
     scope.
   - ADR-NNN (≥0) if the TASK implements an ADR's decision.
   - Module(s) touched (matches RULE-004 names).
   - Skill name (one of the 7 in `.agent-os/skills/`).
   - Command or subagent the implementer will invoke.

4. **Run the Tasks Definition-of-Done checklist** at the end of
   `tasks.md`. Tick each box; flag any that fail.

5. **Diff report** to the user:
   - Path of `tasks.md` and total TASK count.
   - REQ coverage matrix (every REQ-NNN appears in ≥1 TASK?).
   - Dep graph (ASCII).
   - Number of unchecked DoD items.
   - Suggested next command: `/sdd-implement TASK-001`.

6. **Commit**:
   ```
   git add docs/specs/<NNN-slug>/tasks.md
   git commit -m "tasks(<NNN-slug>): N atomic tasks"
   ```

## Anti-patterns to avoid

- DO NOT create TASKs that touch 5+ modules — split them.
- DO NOT use vague AC like "feature works as expected". Use
  Given/When/Then in user-observable terms.
- DO NOT write more TASKs than the Plan's complexity warrants.
- DO NOT proceed to `/sdd-implement` automatically — human approves the
  TASK list first.

## References

- Template: `specs/templates/tasks-template.md`.
- Example output: `specs/examples/loyalty-ledger/tasks.md`.
- Subagent (consumer): `subagents/lg5-test-generator.md` consumes the
  Given/When/Then AC.
