---
description: SDD Plan phase. Read an approved PRD and generate plan.md + ADRs + (when persistent state exists) data-model.md under docs/specs/<NNN-slug>/. Each ADR explicitly states its constitutional impact.
argument-hint: <NNN-feature-slug>
allowed-tools: bash, read, write, edit, glob, grep, task
---

# /sdd-plan

You are running the **Plan** phase of Spec-Driven Development.

> Read first: `docs/specs/<NNN-slug>/prd.md` (must exist and have its DoD
> checklist fully ticked). Then read `rules/CONSTITUTION.md` and the
> entire `rules/` directory: every architectural decision must be
> validated against the 15 constitutional rules.

## Inputs

- `<NNN-feature-slug>` — folder name under `docs/specs/`. If missing or
  the folder doesn't exist, ask.

## Pre-flight

1. Read `docs/specs/<NNN-slug>/prd.md`. If any
   `[NEEDS CLARIFICATION]` markers remain, STOP and report them — they
   must be resolved by the human before planning.
2. Read `.agent-os/rules/CONSTITUTION.md` and every `RULE-*.md`. You
   will cite them by ID in the ADRs.
3. Read `.agent-os/specs/templates/{plan,adr,data-model}-template.md`.
4. Optionally invoke the `lg5-planner` subagent to draft the plan
   structure if the feature is non-trivial (>5 REQs).

## Steps

1. **Identify required ADRs.** For each architectural fork-in-the-road
   the PRD implies (e.g. "saga vs no saga", "consume Avro from upstream
   or redeclare", "JPA vs read-model"), draft one ADR using
   `adr-template.md`. Number them `ADR-001`, `ADR-002`, …

   For each ADR fill the **Constitutional impact** section by listing
   every relevant `RULE-NNN` and stating: confirms / clarifies /
   overrides. If overriding a `must` rule, mark it as time-boxed
   tech-debt.

2. **Generate `plan.md`** from `plan-template.md`:
   - Module map mirrors RULE-004 (the canonical 8 modules).
   - Module ↔ requirement matrix: every REQ-NNN from the PRD MUST be
     covered by ≥1 module. Verify before writing.
   - ADR index: list every file under `adr/`.
   - Sequenced steps: a high-level dep graph; details go in `tasks.md`.
   - Cross-cutting concerns: explicitly assign each to a team/owner.
   - Risks: at minimum re-state any open question from PRD §8 here.

3. **Generate `data-model.md`** from `data-model-template.md` IF the
   feature introduces persistent state, domain events, outbox payloads,
   REST DTOs, or Avro schemas. Skip otherwise (write a 1-line note in
   plan.md saying why).

4. **Run the Plan Definition-of-Done checklist** at the end of
   `plan.md`. Tick each box you can validate; flag the rest.

5. **Diff report** to the user:
   - Path of `plan.md`, `adr/*.md`, optional `data-model.md`.
   - Module ↔ REQ matrix (printed inline).
   - List of constitutional rules each ADR confirms / overrides.
   - Number of unchecked DoD items.
   - Suggested next command: `/sdd-tasks <NNN-feature-slug>`.

6. **Commit**:
   ```
   git add docs/specs/<NNN-slug>/{plan,data-model}.md docs/specs/<NNN-slug>/adr/
   git commit -m "plan(<NNN-slug>): initial plan + N ADRs"
   ```

## Anti-patterns to avoid

- DO NOT introduce technology decisions that the PRD's REQs do not
  require. Every architectural choice must trace back to a REQ.
- DO NOT skip the **Constitutional impact** section in any ADR — even if
  the answer is "no relevant rules". Saying so explicitly is the gate.
- DO NOT silently override a `must` rule. If you must, write a separate
  ADR whose Decision is exactly that override and tag it `tech-debt`.
- DO NOT generate a 200-line plan for a 3-REQ PRD (the
  Verschlimmbesserung trap).
- DO NOT proceed to `/sdd-tasks` automatically — wait for human approval.

## References

- Templates: `specs/templates/{plan,adr,data-model}-template.md`.
- Constitution: `rules/CONSTITUTION.md`.
- Subagent: `subagents/lg5-planner.md`.
- Example output: `specs/examples/loyalty-ledger/{plan.md,adr/,data-model.md}`.
