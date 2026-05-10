# Changelog — lg5-atdd

All notable changes to this skill are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this skill adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The compatibility marker `lg5-spring-sha:` in the frontmatter pins the framework
commit against which the skill was last validated.

## [Unreleased]

## [0.1.0] — 2026-05-09
### Added
- Acceptance-test module dependency template (`lg5-spring-acceptance-test`, `lg5-spring-testcontainers`).
- Cucumber + JUnit Platform Suite bootstrap (`AcceptanceTestCase`, `CucumberHooks` extending `Lg5TestBootPortNone`, `TestContainersLoader`).
- Conditional testcontainers (`testcontainers.<name>.enabled`) for Postgres, Confluent Kafka 7.8.1 + schema registry, Wiremock 3.11.0, and the SUT app container.
- `application-test.yaml` template with image name, server port, traces, and log paths.
- Feature file + step class examples using RestAssured + `Awaitility`.
- Make targets reference (`run-acceptance-test`, `run-at-by-tag`, `run-test-spec`, `run-atdd-module`).
- **Scheduling disabled via YAML only** — explicit note that food-ordering-system never uses `@TestPropertySource` for `scheduling.enabled`.
- **`@Sql` setup/cleanup IT pattern** as the deterministic single-flow alternative to Cucumber.
- Pinned against framework SHA `cbb6783`.
