# Changelog — lg5-spring-overview

All notable changes to this skill are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this skill adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The compatibility marker `lg5-spring-sha:` in the frontmatter pins the framework
commit against which the skill was last validated.

## [Unreleased]

## [0.1.0] — 2026-05-09
### Added
- Initial extraction of the lg5-spring framework module map.
- Recent commit insights (LG-77 stack bump, LG-71 traces+schema-registry, LG-70 outbox status enum, LG-69 testcontainers app config, LG-45 acceptance-test module).
- Configuration prefix cheatsheet (`kafka-config.*`, `kafka-producer-config.*`, `kafka-consumer-config.*`, `<svc>-service.*`, `testcontainers.*`).
- Pinned against framework SHA `cbb6783`.
