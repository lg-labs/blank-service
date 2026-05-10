---
id: RULE-017
slug: build-commands
version: 0.1.0
lg5-spring-sha: d0d754a
severity: should
constitutional: false
scope: build
tags: [make, makefile, build-commands, dev-loop, ci]
description: Use Make targets for repetitive build tasks. Framework — `make all-build`, `make publish-local`. Service — `make install-skip-test`, `make run-avro-model`, `make docker-up`, `make run-apps`, `make run-acceptance-test`.
---

# RULE-017 — Prefer Make targets for build commands

## Statement

Both the lg5-spring framework and consumer services ship a `Makefile` at the
repo root that wraps the canonical Maven/Gradle invocations. Always prefer
the Make target over the raw underlying command:

### Framework (`lg5-spring/Makefile`)

| Target              | What it does                                                  |
|---------------------|---------------------------------------------------------------|
| `make all-build`    | Full Gradle build with all checks; primary CI target.         |
| `make publish-local` | Publishes every framework artifact to the local Maven repo, so consumer services can resolve `lg5-spring-parent` and friends. |

### Consumer service (e.g. `food-ordering-system/Makefile`)

| Target                    | What it does                                                |
|---------------------------|-------------------------------------------------------------|
| `make install-skip-test`  | `mvn install -DskipTests` for fast iteration loops.         |
| `make run-avro-model`     | Regenerates Avro classes from `*-message-model/src/main/resources/avro/*.avsc` (named `make run-kafka-model` in food-ordering-system; both target the same goal). |
| `make docker-up`          | Starts local infra (`<svc>-support/docker-compose.yml` — typically Postgres, Kafka, Schema Registry). |
| `make run-apps`           | Runs the service in dev mode against the docker infra.      |
| `make run-acceptance-test`| Runs the ATDD suite (Cucumber + Testcontainers + Wiremock). |

## Rationale

Make targets are the **interface** between developers and the build tooling:

- They survive build-tool migrations (Gradle → Maven, plugin upgrades) — the
  developer always types `make run-apps`, the underlying invocation can
  change.
- They unify CI and local — the same `make all-build` runs in both, so
  "works on my machine" reduces.
- They document the intended workflow — reading the `Makefile` is the
  fastest way to understand what operations a project supports.
- Agent skills give exact target names, which is unambiguous; raw `mvn
  -pl ... -am ...` commands are harder to copy-paste correctly.

## Example — correct

```bash
# Framework dev loop
cd /tmp/lg5-study/lg5-spring
make all-build
make publish-local

# Consumer service dev loop
cd ~/work/payment-service
make install-skip-test          # quick check after code change
make run-avro-model             # after editing an .avsc
make docker-up                  # bring up postgres + kafka + sr
make run-apps                   # run the service locally
make run-acceptance-test        # full ATDD suite
```

## Anti-pattern

```bash
# WRONG: bypass the Make target with raw maven invocation
mvn -pl payment-container -am install -DskipTests          # ❌ use make install-skip-test

# WRONG: regenerate avro by manually invoking the avro plugin
mvn -pl payment-message-model avro-tools:idl-protocol      # ❌ use make run-avro-model

# WRONG: start infra by editing docker-compose.yml in place and running compose
docker compose -f payment-support/docker-compose.yml up -d # ❌ use make docker-up
```

The anti-patterns work, but they fragment the dev workflow across the team
and make CI scripts diverge from local commands.

## References

- Skill: `lg5-new-service` (Make targets included in the scaffold).
- Skill: `food-ordering-system` (real Makefile examples per service).
- Related rules: RULE-002 (parent POM resolution depends on `make publish-local`),
  RULE-007 (`make run-avro-model` for Avro regeneration).
