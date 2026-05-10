# Changelog — lg5-github-actions

All notable changes to this skill are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this skill adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The compatibility marker `lg5-spring-sha:` in the frontmatter pins the framework
commit against which the skill was last validated.

## [Unreleased]

## [0.1.1] — 2026-05-10
### Security
- Pin `NBprojekt/gource-action@v1.2.1` to its commit SHA
  (`d2fdf85904db416b69445dae5551282528e052ae`) in the `visualization`
  job of `templates/.github/workflows/c-integration.yml`. Mutable tag
  references on third-party (non-verified) actions are flagged by
  Codacy / OpenSSF Scorecard / actionlint as a supply-chain risk; the
  SHA pin makes the dependency immutable while the trailing `# v1.2.1`
  comment preserves human readability. Surfaced by Codacy on consumer
  repo `blank-service` PR #7.

## [0.1.0] — 2026-05-10
### Added
- Initial skill capturing the canonical 11-job CI topology of `blank-service`
  (Setup → Visualization, Checkstyle, Coverage → Quality, Build → Acceptance
  Test → OpenAPI, AsyncAPI, Allure, Docs).
- Shared composite action `setup-maven-credentials` (under
  `templates/.github/actions/setup-maven-credentials/action.yml`) that
  generates `~/.m2/settings.xml` for the GitHub Packages registry of the
  `lg-labs-pentagon` org. Solves the Maven 401 surface in parallel jobs.
- Full `c-integration.yml` workflow template under
  `templates/.github/workflows/c-integration.yml`.
- Documented required secret `PKG_GITHUB_TOKEN` (read:packages on
  `lg-labs-pentagon`) and the env vars the `test` job must set
  (`APPLICATION_LOG_*`, `APPLICATION_TRACES_FILE_ENABLED`).
- Anti-patterns: inline settings.xml, default `GITHUB_TOKEN` for cross-org
  package reads, direct `mvn` invocations bypassing Make targets.
- Pinned against framework SHA `d0d754a`.
