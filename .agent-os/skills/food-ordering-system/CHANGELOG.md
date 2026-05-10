# Changelog — food-ordering-system

All notable changes to this skill are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this skill adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The compatibility marker `lg5-spring-sha:` in the frontmatter pins the framework
commit against which the skill was last validated.

## [Unreleased]

## [0.1.0] — 2026-05-09
### Added
- Module map of the 4 services (order, payment, restaurant, customer) and 5 Kafka topics.
- Topic ↔ outbox table matrix (4 outbox tables across 3 services; customer-service has none).
- End-to-end happy path and compensation paths through the Order saga.
- Per-service deep dives (REST entrypoint, JPA aggregates, Kafka adapters, Saga steps, Outbox tables).
- Cross-cutting patterns (helper-class split, mapper layers, batch listeners with NO-OP, schema-per-service, native PG enums).
- Infrastructure & CI/CD details (`docker-compose`, jib image build, Make targets).
- Pinned against framework SHA `cbb6783`.
