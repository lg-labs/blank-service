# Changelog — lg5-kafka-avro

All notable changes to this skill are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this skill adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The compatibility marker `lg5-spring-sha:` in the frontmatter pins the framework
commit against which the skill was last validated.

## [Unreleased]

## [0.1.0] — 2026-05-09
### Added
- Avro schema (`.avsc`) authoring conventions and namespace rule.
- Producer wiring (`ProducerFactory`, `KafkaTemplate`, `KafkaProducerImpl`) and publisher adapter pattern with `KafkaMessageHelper.getKafkaCallback`.
- Consumer wiring (`ConsumerFactory`, `ConcurrentKafkaListenerContainerFactory`) with batch listener configuration.
- Listener idempotency / NO-OP exception swallowing for `OptimisticLockingFailureException` and not-found exceptions.
- Topic auto-creation via `KafkaAdminClient`.
- `*MessagingDataMapper` layer to keep Avro out of the domain.
- Full reference `application.yaml` block for `kafka-config`, `kafka-producer-config`, `kafka-consumer-config`, and `<svc>-service.*-topic-name`.
- **Listener → Helper delegation** — explicit rule to keep `@Transactional` on the helper, not on the `@KafkaListener` method, so batch-level exceptions can be swallowed without rolling back domain work.
- Pinned against framework SHA `cbb6783`.
