---
description: SDD Specify phase. Convert an informal product prompt into a functional PRD under docs/specs/<NNN-slug>/prd.md, using the prd-template. The PRD is technology-free and ends with a Definition-of-Done checklist.
argument-hint: <feature-slug> "<informal description>"
allowed-tools: bash, read, write, edit, glob
---

# /sdd-specify

You are running the **Specify** phase of Spec-Driven Development for a
service that consumes the `lg5-spring-agent-os` bundle.

> Read first: the bundle's `specs/README.md` (workflow shape) and
> `specs/templates/prd-template.md` (canonical PRD shape).
> Read also: `rules/CONSTITUTION.md` so you know which rules constrain
> later phases — but DO NOT mention them in the PRD itself.

## Inputs

- `<feature-slug>` — kebab-case slug (e.g. `loyalty-ledger`).
- `"<informal description>"` — 2-10 sentences in natural language.
  This is the **stakeholder's words**; capture intent, not technology.

If either is missing, ask the user before doing anything.

## Pre-flight

1. Locate the bundle: `.agent-os/` (submodule) or installed copy.
2. Determine the next feature number `NNN` by scanning
   `docs/specs/` for existing `NNN-*` folders. Use `001` if empty.
3. Create `docs/specs/<NNN>-<feature-slug>/` and an empty `adr/` subdir.
4. Create the feature branch:
   `git switch -c feature/<NNN>-<feature-slug>`.

## Steps

1. **Copy the template**:
   ```
   cp .agent-os/specs/templates/prd-template.md \
      docs/specs/<NNN>-<feature-slug>/prd.md
   ```

2. **Fill in the PRD** from the informal description. Rules:
   - Sections 1-4: write from the user's words; do NOT add technology.
   - Section 5 (Requirements): **decompose** the description into
     atomic `REQ-NNN` rows. Each row is one thing the system must do
     (action verb in active voice) + one observable acceptance.
   - Section 6 (Out of scope): infer at least 1-2 items the user did
     NOT mention but a reader might assume.
   - Section 7: feature-level AC (3-6 outcomes). Examples: "all listed
     scenarios pass ATDD", "no `must` rule violations".
   - Section 8 (Open questions): mark every ambiguity as
     `[NEEDS CLARIFICATION: <question>] | <decider>`. Be aggressive —
     surfacing ambiguity here is the whole point of Specify.

3. **Run the PRD Definition-of-Done checklist** at the end of the
   template. Tick each box you can validate yourself; flag the rest.

4. **Diff report**: print to the user
   - Path of the generated PRD.
   - Number of REQ-NNN created.
   - Number of `[NEEDS CLARIFICATION]` markers.
   - Number of unchecked DoD items (with reasons).
   - Suggested next command: `/sdd-plan <NNN>-<feature-slug>` once the
     user has resolved the clarifications.

5. **Commit**:
   ```
   git add docs/specs/<NNN>-<feature-slug>/prd.md
   git commit -m "specify(<NNN>-<feature-slug>): initial PRD draft"
   ```

## Anti-patterns to avoid

- DO NOT mention Spring, Kafka, REST, Postgres, Avro, or any module name
  in the PRD. That is what the Plan / ADRs are for.
- DO NOT invent acceptance criteria the user did not imply ("16 AC for a
  3-AC feature" — the Verschlimmbesserung trap from Fowler's article).
- DO NOT skip the `[NEEDS CLARIFICATION]` step. A clean PRD is suspect.
- DO NOT modify code or other docs outside `docs/specs/<NNN>-<slug>/`.
- DO NOT proceed to `/sdd-plan` automatically — Specify ends at the
  human-approval gate.

## References

- Template: `specs/templates/prd-template.md`.
- Workflow: `specs/README.md`.
- Why: Fowler, _Understanding Spec-Driven-Development_,
  https://martinfowler.com/articles/exploring-gen-ai/sdd-3-tools.html.
