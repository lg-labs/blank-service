# Changelog — lg5-spring-agent-os rules bundle

All notable changes to the **rules** artifact set are documented here.
This file uses [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
[SemVer 2.0.0](https://semver.org/spec/v2.0.0.html).

The version below tracks the rules bundle as a whole, independently of
individual rule versions in their own frontmatter.

## [0.3.2] — 2026-05-10
### Changed
- Framework SHA pin bumped from `af81c7c` to `d0d754a` (PATCH).
- Includes [`fix(testcontainers)`: in-network Kafka listener](https://github.com/lg-labs-pentagon/lg5-spring/pull/1)
  — relevant for RULE-007 and RULE-013 (Kafka/testcontainers scope) but
  no rule wording changed.
- Also pulls in [LG-83] Jib Maven plugin upgrade to 3.5.1 (transitive on
  the framework parent pom).
- All 18 rule files updated `lg5-spring-sha: d0d754a` in frontmatter.
  RULE-001 and RULE-002 example POM coordinates updated to
  `1.0.0-alpha.d0d754a`.
### Notes
- **No rule wording changed.** RULE-001 still mandates Spring Boot 3.4.2
  (which remains true on `d0d754a`).

## [0.3.1] — 2026-05-10
### Changed
- Framework SHA pin bumped from `cbb6783` to `af81c7c` to honor RULE-001's
  Spring Boot 3.4.2 requirement (`cbb6783` actually shipped 3.3.5,
  discovered during consumer-service TASK-002 of `lg5-loyalty-ledger`).
- `bundle.version` in `manifest.yaml` bumped to `0.3.1` (PATCH; cross-bundle
  invariant requires every per-type manifest to agree).
- All 18 rule files updated `lg5-spring-sha: af81c7c` in frontmatter.
  RULE-001 and RULE-002 example POM coordinates updated to
  `1.0.0-alpha.af81c7c`.
### Notes
- **No rule wording changed.** RULE-001 still mandates Spring Boot 3.4.2
  (which is now actually true on the pinned framework SHA).
- Bundles in `af81c7c`: Spring Boot 3.4.2 upgrade (`e5139d0`),
  `ConfluentKafkaContainerCustomConfig` (`5fb16aa`), CI/docs updates.

## [0.2.0] — 2026-05-09
### Added
- **Constitution layer** (concept borrowed from spec-kit). 15 of the 18 rules
  (those with `severity: must`) are now flagged as **constitutional** — i.e.
  immutable architectural laws that any consumer service must respect or
  explicitly justify in an ADR.
- New `constitutional: bool` field in every rule's frontmatter.
- New `rules/CONSTITUTION.md` document: index of the 15 constitutional rules
  with their one-liner, plus rules of engagement (how PRDs/Plans/Tasks should
  reference the constitution).
- New `constitution: CONSTITUTION.md` field in `manifest.yaml`.
- Per-rule `constitutional: <bool>` echoed in `manifest.yaml` for fast lookup.
### Notes
- No rule wording changed. This is a metadata + documentation enhancement.
- Bundle bumped to `0.3.0` because (a) the SDD-aligned commands and templates
  introduced in the same release require this metadata, and (b) the
  cross-bundle invariant requires `bundle.version` to match across types.

## [0.1.0] — 2026-05-09
### Added
- Initial extraction of the 18 always-active hard rules from the workspace
  `AGENTS.md` into individual `RULE-NNN-<slug>.md` files. Each rule carries
  YAML frontmatter (`id`, `version`, `lg5-spring-sha`, `severity`, `scope`,
  `tags`, `description`) so agents and CI can address them by stable ID.
- `manifest.yaml` listing all 18 rules with id ↔ slug ↔ severity ↔ scope.
- `AGENTS.md` was rewritten to be a thin index that delegates the full rule
  texts to this directory.
### Notes
- Validated against `lg5-spring` SHA `cbb6783`.
- Severity vocabulary: `must` (hard), `should` (default), `info` (reference).
- Scope vocabulary: `framework`, `architecture`, `kafka`, `outbox`, `saga`,
  `testing`, `style`, `build`, `reference`.
