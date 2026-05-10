---
id: RULE-018
slug: reference-projects
version: 0.1.0
lg5-spring-sha: d0d754a
severity: info
constitutional: false
scope: reference
tags: [reference, repos, framework, examples, blank-service]
description: Three canonical reference projects — `lg5-spring` (framework), `food-ordering-system` (real example), `blank-service` (skeleton). Clone them under `/tmp/lg5-study/` to provide grounded answers.
---

# RULE-018 — Reference projects

## Statement

The agent — and any human reasoning about lg5 conventions — must ground
non-trivial answers against three canonical repositories. The convention is
to clone them under `/tmp/lg5-study/` so the agent can `Read`/`Grep` them at
will:

| Repo               | URL                                                       | Role |
|--------------------|-----------------------------------------------------------|------|
| `lg5-spring`       | https://github.com/lg-labs-pentagon/lg5-spring            | The framework itself. The single source of truth for what classes/interfaces exist. |
| `food-ordering-system` | https://github.com/lg-labs/food-ordering-system       | The most complete real-world example: order, payment, restaurant, customer services with REST + JPA + Kafka producer/consumer + Saga + Outbox + ATDD. Use this when copying patterns. |
| `blank-service`    | https://github.com/lg-labs/blank-service                  | The bare skeleton for a new microservice. Use this when scaffolding. |

If you propose a class, annotation, or pattern that you cannot point to in
one of these repos, **say so explicitly** ("I am unsure — this class isn't
in any of the cloned repos"). Do not invent.

## Rationale

The lg5 ecosystem evolves quickly and not everything is documented. Three
guarantees come from grounding against these repos:

1. **No fabrication.** Every framework class the agent mentions is real and
   importable; every annotation is one that actually exists.
2. **Pattern fidelity.** When a question is "how do I do X", the agent can
   point at exactly how `food-ordering-system` does it instead of inventing
   a plausible-but-wrong shape.
3. **Reproducibility across sessions.** A new agent session can re-derive
   the same answers by re-reading the same files; nothing depends on
   memorized prior knowledge that may have drifted.

The `/tmp/lg5-study/` location is chosen because:

- It survives across agent sessions on the same machine.
- It is unambiguously **NOT committed** anywhere — no risk of accidentally
  shipping it.
- It is allowlisted by the bundle's `validate.sh` so prose references in
  skills/rules to `/tmp/lg5-study/...` paths are accepted.

## Example — correct

```bash
# Initial setup (do this once per machine)
mkdir -p /tmp/lg5-study
cd /tmp/lg5-study
git clone https://github.com/lg-labs-pentagon/lg5-spring.git
git clone https://github.com/lg-labs/food-ordering-system.git
git clone https://github.com/lg-labs/blank-service.git
```

In an agent answer:

> "The outbox helper pattern follows `food-ordering-system/order-service/
> order-application-service/src/main/java/.../outbox/PaymentOutboxHelper.java`
> — see how `getPaymentOutboxMessageBySagaIdAndSagaStatus` returns
> `Optional<List<...>>` so the caller can short-circuit when empty (RULE-009)."

In a code block inside a skill:

```text
/tmp/lg5-study/food-ordering-system/order-service/.../outbox/PaymentOutboxHelper.java
/tmp/lg5-study/lg5-spring/lg5-spring-saga/src/main/java/com/lg5/spring/saga/SagaStep.java
```

(both are allowlisted by the validator).

## Anti-pattern

```text
# WRONG: any /tmp/ path inside a code block that is NOT under /tmp/lg5-study/
/tmp/my-experiments/payment-svc/...        # ❌ blocked by validate.sh

# WRONG: invent a file or class without a citation
"You can use the framework's @SagaParticipant annotation."   # ❌ doesn't exist
```

## References

- Skill: `lg5-spring-overview` (gives a framework module map sourced from
  the `lg5-spring` clone).
- Skill: `food-ordering-system` (deep dive on the reference implementation).
- Skill: `lg5-new-service` (uses `blank-service` as the scaffolding source).
- Related rules: RULE-002 (parent POM SHA comes from `lg5-spring` HEAD),
  RULE-005 (anti-fabrication on annotations).
