# Changelog — lg5-saga

All notable changes to this skill are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this skill adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The compatibility marker `lg5-spring-sha:` in the frontmatter pins the framework
commit against which the skill was last validated.

## [Unreleased]

## [0.1.0] — 2026-05-09
### Added
- `SagaStep<T>` contract description and mental model.
- Canonical `OrderPaymentSaga` example with `process` / `rollback` and idempotency guard.
- Mandatory rules: `@Component`, `@Transactional`, ctor injection, idempotency-by-outbox-lookup, optimistic locking, no-rethrow on `OptimisticLockingFailureException`.
- Per-service `SagaStatus` enum reference.
- End-to-end flow diagram (Order create → payment-request → payment-response → approval-request).
- **Command-Handler / Helper split** — Pattern A (handler+helper, order-service) and Pattern B (thin listener + transactional helper, payment-service).
- **`ApplicationEventDomainPublisher` is vestigial** — explicit warning that no `@TransactionalEventListener` consumes it in food-ordering-system; outbox is written synchronously inside the helper.
- Pinned against framework SHA `cbb6783`.
