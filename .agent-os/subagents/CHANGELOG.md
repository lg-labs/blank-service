# Changelog — lg5-spring-agent-os subagents bundle

All notable changes to the **subagents** artifact set are documented here.
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
- **No subagent behavior changed** in this release. Individual subagent
  versions are unchanged.

## [0.3.1] — 2026-05-10
### Changed
- Framework SHA pin bumped from `cbb6783` to `af81c7c` to honor RULE-001's
  Spring Boot 3.4.2 requirement (`cbb6783` actually shipped 3.3.5,
  discovered during consumer-service TASK-002 of `lg5-loyalty-ledger`).
- `bundle.version` in `manifest.yaml` bumped to `0.3.1` (PATCH; cross-bundle
  invariant requires every per-type manifest to agree).
### Notes
- **No subagent behavior changed** in this release. Individual subagent
  versions are unchanged.

## [0.2.0] — 2026-05-09
### Changed
- `manifest.yaml` `bundle.version` bumped to `0.3.0` to align with the
  rest of the bundle (cross-bundle invariant).
### Notes
- **No subagent content changed in this release.** All 3 subagents remain
  at individual version `0.1.0`.
- See `rules/CHANGELOG.md`, `specs/CHANGELOG.md`, and
  `commands/CHANGELOG.md` for the substantive 0.3.0 changes (constitution
  layer, SDD templates, SDD orchestrator commands).

## [0.1.0] — 2026-05-09
### Added
- `lg5-code-reviewer` — reviews diffs against the 18 hard rules and cites
  violations by stable RULE-ID.
- `lg5-test-generator` — generates IT/ATDD test scaffolds following
  RULE-012 (test profiles + base classes) and RULE-013 (Testcontainers gating).
- `lg5-planner` — decomposes a feature request into a step-by-step
  implementation plan grounded in the bundle's rules and skills.
### Notes
- Validated against `lg5-spring` SHA `cbb6783`.
- Subagents are written for OpenCode's agent format (YAML frontmatter with
  `description`, `tools`, `model`); they should be portable to Claude Code's
  subagent format with minor adaptation.
