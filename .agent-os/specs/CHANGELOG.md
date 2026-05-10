# Changelog — lg5-spring-agent-os specs bundle

All notable changes to the **specs** artifact set are documented here.
Uses [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
[SemVer 2.0.0](https://semver.org/spec/v2.0.0.html).

## [0.3.2] — 2026-05-10
### Changed
- Framework SHA pin bumped from `af81c7c` to `d0d754a` (PATCH).
- Includes [`fix(testcontainers)`: in-network Kafka listener](https://github.com/lg-labs-pentagon/lg5-spring/pull/1)
  — surfaced while wiring the first downstream Kafka listener IT in
  `lg5-loyalty-ledger` TASK-009.
- Also pulls in [LG-83] Jib Maven plugin upgrade to 3.5.1 (transitive on
  the framework parent pom).
### Notes
- **No spec template or example content changed** in this release.

## [0.3.1] — 2026-05-10
### Changed
- Framework SHA pin bumped from `cbb6783` to `af81c7c` to honor RULE-001's
  Spring Boot 3.4.2 requirement (`cbb6783` actually shipped 3.3.5,
  discovered during consumer-service TASK-002 of `lg5-loyalty-ledger`).
- `bundle.version` in `manifest.yaml` bumped to `0.3.1` (PATCH; cross-bundle
  invariant requires every per-type manifest to agree).
### Notes
- **No spec template or example content changed** in this release.

## [0.3.0] — 2026-05-09
### Added
- **Spec-Driven Development workflow** formalized following Fowler/spec-kit:
  Specify → Plan → Tasks → Implement, each phase consuming a template
  and producing a per-feature markdown under `docs/specs/<NNN-slug>/`.
- New templates under `templates/`:
  - `plan-template.md` — technical plan (module map, ADR index,
    dependency graph, risks, DoD checklist).
  - `tasks-template.md` — atomic TASK-NNN with Given/When/Then AC and
    Definition of Done checklist.
  - `data-model-template.md` — concrete shapes (aggregates, events,
    outbox payloads, REST DTOs, Avro schemas, JPA tables).
  - `research-template.md` — optional time-boxed spike doc.
- `specs/README.md` documenting the SDD workflow and per-feature folder
  layout for consumer services.
- The illustrative `loyalty-ledger` example was **split** into a
  per-feature folder under `examples/loyalty-ledger/`:
  `prd.md`, `plan.md`, `tasks.md`, `data-model.md`, `README.md`,
  `adr/ADR-001-outbox-only-no-saga.md`,
  `adr/ADR-002-reuse-order-message-model.md`. This shape is what
  consumer services replicate under `docs/specs/<NNN-slug>/`.
### Changed
- `prd-template.md` rewritten: requirements now use stable `REQ-NNN` IDs;
  Definition of Done checklist embedded; the template explicitly forbids
  technology mentions to keep PRDs purely functional.
- `adr-template.md`: the "lg5 rule cross-references" section is renamed
  to **"Constitutional impact"** to align with the new constitution
  vocabulary (see `rules/CONSTITUTION.md`); DoD checklist embedded.
- `manifest.yaml` reorganized: explicit `templates/` and `examples/`
  groupings; example entry now points to a folder.
- `validate.sh` updated to walk `templates/` + recurse into
  `examples/<feature>/` and to accept the new `example-*` `kind` values.
### Removed
- Old monolithic `examples/microservice-spec-example.md` (split into the
  per-feature folder above; nothing lost in content).
### Notes
- Validated against `lg5-spring` SHA `cbb6783`.
- Inspired by the spec-kit per-feature folder shape and DoD checklists.

## [0.1.0] — 2026-05-09
### Added
- `prd-template.md` — Product Requirements Document template with sections
  for problem, users, success metrics, scope, out-of-scope, dependencies,
  and acceptance criteria.
- `adr-template.md` — Lightweight ADR template (context, decision,
  alternatives, consequences) with a "lg5 rule cross-references" section.
- `examples/microservice-spec-example.md` — End-to-end spec example for a
  hypothetical `loyalty-ledger` service combining the PRD + ADRs + module
  breakdown.
### Notes
- Validated against `lg5-spring` SHA `cbb6783`.
- Spec format is plain markdown with YAML frontmatter (`kind`, `version`,
  `description`); designed to be filled in by humans or by the
  `lg5-planner` subagent.
