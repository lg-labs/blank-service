# Changelog — lg5-allure-report

All notable changes to this skill are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this skill adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The compatibility marker `lg5-spring-sha:` in the frontmatter pins the framework
commit against which the skill was last validated.

## [Unreleased]

## [0.1.0] — 2026-05-10
### Added
- Initial skill capturing the Allure Report wiring for the
  `<svc>-acceptance-test` module of an lg5-spring service.
- Maven dependencies (`allure-cucumber7-jvm` + `allure-junit-platform`,
  both `2.29.1`) and the Cucumber plugin registration in
  `AcceptanceTestCase` (`io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm`).
- `templates/src/test/resources/allure.properties` pinning the results
  directory to `target/allure-results`.
- CI job `allure` with `if: always()`, Allure CLI 2.32.0 download, and
  the matching `Upload Allure raw results` step the upstream `test` job
  must publish.
- Anti-patterns: `allure-maven-plugin` coupling, replacing legacy
  Cucumber HTML, floating CLI version.
- Pinned against framework SHA `d0d754a`.
