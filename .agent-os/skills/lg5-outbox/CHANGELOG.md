# Changelog — lg5-outbox

All notable changes to this skill are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this skill adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The compatibility marker `lg5-spring-sha:` in the frontmatter pins the framework
commit against which the skill was last validated.

## [Unreleased]

## [0.1.0] — 2026-05-09
### Added
- Framework abstractions (`OutboxStatus`, `OutboxScheduler`) and per-service implementation responsibilities.
- JPA outbox entity shape with mandatory `@Version` for optimistic locking.
- DDL with native Postgres enum types (`outbox_status`, `payment_status`, `approval_status`) and `jsonb` payload column.
- Schema-per-service convention (`order` quoted, `payment`, `restaurant`).
- Repository, Helper, Scheduler, and Cleanup-Scheduler patterns.
- Status transition diagram (`STARTED → COMPLETED|FAILED`, saga-status progression).
- **JPA-vs-DDL asymmetry warning** — `payload` is `String` in JPA but `jsonb` in DDL; status enums are `EnumType.STRING` in JPA but native PG enums in DDL. Do **not** add `@JdbcTypeCode(SqlTypes.JSON)`.
- **Payload-vs-Domain-Event split** — domain event lives in `*-domain-core`; outbox payload is a flat Lombok DTO in `*-application-service/.../outbox/model/`.
- **`@Sql` integration test pattern** — canonical `OrderPaymentSagaIT` location (`*-container`, not `*-data-access`) with setup/cleanup SQL files.
- Pinned against framework SHA `cbb6783`.
