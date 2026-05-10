---
description: SDD Implement phase. Execute ONE task from tasks.md (write code + tests, commit, mark done). Loops by re-invocation.
argument-hint: <TASK-NNN> [<NNN-feature-slug>]
allowed-tools: bash, read, write, edit, glob, grep, task
---

# /sdd-implement

You are running the **Implement** phase of Spec-Driven Development.
**One TASK per invocation.** This is intentional: small, reversible
steps in line with Fowler's iteration warning.

> Read first: the constitution (`rules/CONSTITUTION.md`) and every
> `RULE-*.md`. They are immutable for this work.

## Inputs

- `<TASK-NNN>` — the task ID to implement (e.g. `TASK-002`).
- `<NNN-feature-slug>` — optional; if omitted, infer from the current
  branch (`feature/<NNN-slug>`).

## Pre-flight

1. Read `docs/specs/<NNN-slug>/tasks.md` and locate the section for
   `<TASK-NNN>`. If not found, STOP.
2. Verify the TASK's `Status` is `todo`. If `in_progress` or `done`,
   STOP and ask the user.
3. Verify all `Depends on:` TASKs are `done`. If not, STOP.
4. Read the referenced REQ-NNN, RULE-NNN, ADR-NNN, and the named skill
   under `.agent-os/skills/`.
5. Mark the TASK's `Status` to `in_progress` in `tasks.md`.

## Steps

1. **Plan locally** (do NOT update files yet). Sketch the diff in your
   head: which files will be created, which modified, which deleted.
   Call out any dependency on other TASKs that should have been listed.

2. **Invoke the building-block command** if one is named in the TASK
   (`/scaffold-service`, `/add-saga`, `/add-outbox`,
   `/add-kafka-listener`). Otherwise write the code directly using the
   skill as your reference.

3. **Write tests** that match the Given/When/Then AC of the TASK. If
   the TASK references RULE-012/RULE-013 (testing rules), invoke the
   `lg5-test-generator` subagent to draft them.

4. **Run the local verifier** (TASK-specific):
   - Build: `make install-skip-test` MUST succeed.
   - Tests: the new tests MUST pass.
   - The Given/When/Then AC MUST be satisfied.

5. **Run `lg5-code-reviewer` subagent** on the diff. Resolve any
   `must`-severity findings before commit. `should`/`info` findings
   may be deferred — note them in the commit body.

6. **Update `tasks.md`**: set `Status: done` for this TASK; append a
   one-line completion note with the commit SHA placeholder.

7. **Commit** following Conventional Commits, embedding the TASK ID:
   ```
   git add -A
   git commit -m "feat(<TASK-NNN>): <task title>

   - implements <REQ-NNN>, <REQ-MMM>
   - touches modules <module>, <module>
   - <subagent findings or 'no findings'>
   "
   ```
   Then update `tasks.md` with the actual commit SHA and amend (only
   safe if no other commit happened in between — see git safety
   guidelines).

8. **Diff report** to the user:
   - TASK ID + new status.
   - Files touched.
   - Test results summary.
   - `lg5-code-reviewer` findings.
   - Suggested next command: `/sdd-implement TASK-<NNN+1>` (or the
     next `todo` TASK whose deps are now satisfied).

## Anti-patterns to avoid

- DO NOT implement more than one TASK per invocation. The whole point
  of SDD's per-task gate is to allow human review between commits.
- DO NOT skip step 5 (`lg5-code-reviewer`). The constitution must be
  enforced at commit time, not at PR time.
- DO NOT introduce technology that no ADR justifies. If you discover
  an unforeseen need, STOP and propose a new ADR via `/sdd-plan`
  amendment.
- DO NOT modify `prd.md`, `plan.md`, ADRs, or `tasks.md` other than
  flipping the Status field and appending the completion line. If they
  need real changes, that's a re-Plan event.

## References

- Constitution: `rules/CONSTITUTION.md`.
- Subagents: `subagents/{lg5-code-reviewer,lg5-test-generator}.md`.
- Building blocks: `commands/{scaffold-service,add-saga,add-outbox,add-kafka-listener}.md`.
- Skills: `skills/<name>/SKILL.md`.
