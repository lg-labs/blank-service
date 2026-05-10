# Changelog — lg5-spring-agent-os commands bundle

All notable changes to the **commands** artifact set are documented here.
Uses [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
[SemVer 2.0.0](https://semver.org/spec/v2.0.0.html).

## [0.3.3] — 2026-05-10
### Added
- New building-block command **`/scaffold-ci-cd`** (v0.1.0) that
  installs the CI/CD pipeline into a consumer service by copying the
  templates shipped by the new `lg5-github-actions`, `lg5-api-docs`,
  and `lg5-allure-report` skills (workflow, composite action, Swagger
  UI / AsyncAPI HTML wrappers, Allure properties). Also performs the
  in-place edits in `<svc>-acceptance-test/pom.xml` and
  `AcceptanceTestCase.java` that Allure requires. Pre-flight ensures
  the bundle is installed at `.agent-os/`.
### Notes
- **No existing command behavior changed** in this release.

## [0.3.2] — 2026-05-10
### Changed
- Framework SHA pin bumped from `af81c7c` to `d0d754a` (PATCH).
- Includes [`fix(testcontainers)`: in-network Kafka listener](https://github.com/lg-labs-pentagon/lg5-spring/pull/1)
  — surfaced while wiring the first downstream Kafka listener IT in
  `lg5-loyalty-ledger` TASK-009.
- Also pulls in [LG-83] Jib Maven plugin upgrade to 3.5.1 (transitive on
  the framework parent pom).
### Notes
- **No command behavior changed** in this release. Individual command
  versions are unchanged.

## [0.3.1] — 2026-05-10
### Changed
- Framework SHA pin bumped from `cbb6783` to `af81c7c` to honor RULE-001's
  Spring Boot 3.4.2 requirement (`cbb6783` actually shipped 3.3.5,
  discovered during consumer-service TASK-002 of `lg5-loyalty-ledger`).
- `bundle.version` in `manifest.yaml` bumped to `0.3.1` (PATCH; cross-bundle
  invariant requires every per-type manifest to agree).
### Notes
- **No command behavior changed** in this release. Individual command
  versions are unchanged.

## [0.2.0] — 2026-05-09
### Added
- **SDD orchestrator commands** (4 new) that drive the Spec-Driven
  Development workflow phases per Fowler/spec-kit:
  - `/sdd-specify <feature-slug> "<informal description>"` — produces a
    functional, technology-free PRD under `docs/specs/<NNN-slug>/prd.md`.
  - `/sdd-plan <NNN-feature-slug>` — produces `plan.md` + `adr/*.md`
    (and `data-model.md` if persistent state); each ADR explicitly
    states its constitutional impact by RULE-ID.
  - `/sdd-tasks <NNN-feature-slug>` — decomposes the Plan into atomic
    `TASK-NNN` with Given/When/Then acceptance criteria and a
    Definition-of-Done checklist.
  - `/sdd-implement <TASK-NNN>` — executes ONE task at a time (write
    code + tests, run `lg5-code-reviewer`, commit with `TASK-NNN` ID).
- New `category` field in `manifest.yaml`: `sdd` vs `building-block`.
### Changed
- Existing 4 commands re-categorized as `building-block` (invoked from
  inside `/sdd-implement`, not directly by humans in the SDD flow).
### Notes
- Bundle bumped to `0.3.0` to align with the rules + specs co-release.
- The SDD commands assume the bundle is mounted at `.agent-os/` in the
  consumer repo (git-submodule mode).

## [0.1.0] — 2026-05-09
### Added
- `/scaffold-service` — bootstrap a new microservice from `blank-service`.
- `/add-saga` — add an end-to-end SagaStep (publisher + listener + outbox + scheduler).
- `/add-outbox` — add a Transactional Outbox (entity + DDL + helper + scheduler) for one event type.
- `/add-kafka-listener` — add a batch Kafka listener with NO-OP exception
  handling per RULE-010.
### Notes
- Validated against `lg5-spring` SHA `cbb6783`.
- Commands are written for OpenCode's slash-command format (YAML frontmatter
  with `description`, `argument-hint`, `allowed-tools`); they should be
  portable to Claude Code and Cursor with minor adaptation.
