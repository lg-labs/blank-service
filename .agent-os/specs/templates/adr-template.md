---
kind: template
name: adr-template
version: 0.2.0
description: Lightweight Architecture Decision Record template aware of the lg5-spring constitution.
---

# ADR-<NNN>: <Short title in present tense>

- **Status:** Proposed | Accepted | Deprecated | Superseded by ADR-MMM
- **Date:** <YYYY-MM-DD>
- **Deciders:** <names>
- **Consulted:** <names>
- **Informed:** <names>

## Context

What is the situation that motivates a decision? Include:

- The system state today (briefly — link to architecture diagrams).
- The forces that make a decision necessary (new requirement, scaling
  pain point, framework upgrade, etc.).
- The constraints that any decision must satisfy (typically: the 18 hard
  rules of `lg5-spring-agent-os`).

## Decision

State the decision clearly in 1-3 sentences. Use active voice — "We will
…" — not "It is recommended that …".

## Alternatives considered

For each alternative seriously evaluated, document:

- **<Alternative A>** — short description.
  - Pros: …
  - Cons: …
  - Why not chosen: …

- **<Alternative B>** — …

(If only one option was considered, the decision wasn't really needed —
either expand the alternatives or downgrade this from an ADR to a
documentation note.)

## Consequences

What changes after this decision is implemented?

- **Positive:** what gets better, what new options open up.
- **Negative:** what gets worse, what new constraints appear.
- **Neutral:** trade-offs that aren't strictly better or worse.

## Constitutional impact

Which of the 15 **constitutional** rules (severity `must`) does this
decision interact with? List them by ID with a one-line note on the
relationship.

- RULE-XXX — <how this ADR confirms / clarifies / temporarily overrides this rule>.
- …

If this ADR proposes overriding a `must`-level rule, that override MUST be
time-boxed and tracked as a separate technical-debt item linked here.

Advisory rules (`should`, `info`) do **not** need to be listed unless the
decision changes how the team treats them.

## Implementation notes

Optional. Pointers to:

- The PRD this ADR supports (specs/<prd-name>.md).
- The implementation plan (specs/<plan-name>.md).
- The slash commands that will be invoked (`/scaffold-service`, `/add-saga`, …).
- The PR(s) that implement this.

## Related ADRs

- ADR-XXX — <title>.

## Definition of Done (ADR)

- [ ] Status is one of `Proposed | Accepted | Deprecated | Superseded`.
- [ ] Decision is stated in active voice ("We will…").
- [ ] At least one alternative is documented (otherwise it is not a real
      decision — downgrade to a doc note).
- [ ] Consequences cover positive AND negative.
- [ ] Constitutional impact section names every relevant `must` rule.
- [ ] Any `must` override is time-boxed with a tech-debt link.

---

_Originally drafted: <YYYY-MM-DD> · Last reviewed: <YYYY-MM-DD>._
