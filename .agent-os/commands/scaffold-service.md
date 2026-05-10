---
description: Scaffold a new lg5-spring microservice from the blank-service skeleton, renaming groupId/artifactId/packages to the user-provided service name.
argument-hint: <service-name> [groupId]
allowed-tools: bash, read, write, edit, glob, grep
---

# /scaffold-service

You are scaffolding a brand-new microservice on top of the lg5-spring framework
by adapting the [`blank-service`](https://github.com/lg-labs/blank-service)
skeleton. Follow the rules in `rules/RULE-001` through `RULE-018` strictly.

## Inputs

- `<service-name>` — the new service name in lowercase-hyphen form
  (e.g. `payment`, `loyalty-ledger`). Used as the artifactId prefix and the
  package leaf.
- `<groupId>` — optional Maven groupId; defaults to `com.example.<service-name>`.

If the user provided fewer arguments than required, ask them for the missing
ones BEFORE making any file changes.

## Pre-flight checks

1. Verify `/tmp/lg5-study/blank-service/` exists. If not, ask the user to
   `git clone https://github.com/lg-labs/blank-service.git /tmp/lg5-study/blank-service`
   first (RULE-018).
2. Verify the consumer repo's `AGENTS.md` references the bundle (`lg5-spring-agent-os`).
   If not, the project may not be set up for this scaffold — confirm with the user.
3. Determine the latest `lg5-spring` short SHA: read it from
   `<bundle>/skills/manifest.yaml` `bundle.lg5-spring-sha`. Use that for
   the parent POM version (RULE-002).

## Steps

For each step, before executing the underlying commands, summarize what you
are about to do in 1 sentence. After executing, summarize what was created
in ≤3 bullet points.

1. **Copy the skeleton** into the consumer repo at the new service name:
   ```bash
   cp -R /tmp/lg5-study/blank-service ./<service-name>
   ```
   then remove `.git/` from the copy.

2. **Rename modules and directories** from `blank-*` → `<service-name>-*`.
   The 8 modules to mirror (RULE-004):
   ```
   <service-name>-domain/{<service-name>-domain-core, <service-name>-application-service}
   <service-name>-api
   <service-name>-data-access
   <service-name>-message/{<service-name>-message-core, <service-name>-message-model}
   <service-name>-external           (only if user confirms they need Feign)
   <service-name>-container
   <service-name>-acceptance-test
   <service-name>-support
   ```
   Use `glob` + `bash mv` for directory renames. Use `grep` + `edit` to
   replace `blank-` → `<service-name>-` and `com.blanksystem` → `<groupId>`
   in every `pom.xml`, every `.java` file, every YAML.

3. **Update the parent POM version** in the root `pom.xml` to
   `1.0.0-alpha.<lg5-spring-sha>` (RULE-002).

4. **Create the canonical Make targets** in `Makefile` at the new service
   root. Mirror the food-ordering-system Makefile but with the new service
   name (RULE-017). At minimum:
   - `install-skip-test`
   - `run-avro-model`
   - `docker-up` / `docker-down`
   - `run-apps`
   - `run-acceptance-test`

5. **Create application.yaml** at `<service-name>-container/src/main/resources/`
   with the canonical config prefixes (RULE-014):
   ```yaml
   kafka-config: { bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:19092} }
   <service-name>-service:
     # business config goes here
   scheduling: { enabled: true }
   ```

6. **Create application-test.yaml + application-local.yaml** with the
   appropriate overrides for IT and dev profiles (RULE-012, RULE-013).

7. **Sanity build**: from the new service root run `make install-skip-test`.
   It MUST succeed before you declare the scaffold complete.

8. **Final report**: print
   - the new module tree (`tree -L 2 <service-name>/`)
   - the parent POM version pinned
   - the Make targets created
   - the next 3 recommended commands (`/add-outbox`, `/add-kafka-listener`,
     `/add-saga`) the user might invoke next.

## Anti-patterns to avoid

- DO NOT add custom annotations like `@LgController` (RULE-005).
- DO NOT skip any of the 8 modules. `<service-name>-external` is the only
  optional one and only if Feign clients are needed.
- DO NOT use a free-form parent POM version (RULE-002).
- DO NOT put Spring annotations in `<service-name>-domain-core` (RULE-003).
- DO NOT rename modules to non-canonical shapes (`-rest`, `-app`,
  `-persistence`) — the framework's auto-config and the agent skills assume
  the canonical names (RULE-004).

## References

- Skill: `lg5-new-service` (full step-by-step walkthrough with the exact
  Maven coordinates and pom.xml diffs).
- Skill: `food-ordering-system` (real-world groupId/package conventions).
- Rules: RULE-002, RULE-003, RULE-004, RULE-014, RULE-017.
