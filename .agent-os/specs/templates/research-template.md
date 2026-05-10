---
kind: template
name: research-template
version: 0.1.0
description: Time-boxed spike / discovery doc. Optional companion to a Plan.
---

# Research — `<question or topic>`

> Optional document. Use when the Plan has an open question that requires
> a spike (PoC, benchmark, library evaluation) before a decision can be
> made. Outcome usually feeds into a new ADR.

## Question

State the single question this research must answer. One sentence.

## Context

Why is this question open? What's blocked until we answer it? Link the
PRD section / Plan risk / ADR draft.

## Time-box

- **Started:** <YYYY-MM-DD>
- **Hard stop:** <YYYY-MM-DD>
- **Owner:** <name>

## Options considered

| Option | Pros | Cons | Effort | Verdict |
|--------|------|------|--------|---------|
| `<A>` | … | … | S/M/L | … |
| `<B>` | … | … | S/M/L | … |

## Findings

What the spike actually showed. Numbers, screenshots, links to PoC
branches. Be ruthless about pruning — only what informs the decision.

## Recommendation

One paragraph. Which option do we pick and why?

## Decision

- [ ] Captured in **ADR-NNN** at `<link>` — _(if a real architectural
      decision; otherwise leave unchecked)_
- [ ] No ADR needed — _(if just an info finding; explain why)_

## Definition of Done (Research)

- [ ] Question is one sentence.
- [ ] Time-box was respected (or explicitly extended once, with reason).
- [ ] At least 2 options compared.
- [ ] Recommendation is concrete (no "it depends" without a follow-up).
- [ ] Outcome is either an ADR or an explicit "no ADR needed" note.
