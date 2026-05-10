---
id: RULE-005
slug: no-custom-annotations
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: framework
tags: [annotations, stock-spring, lombok, no-magic]
description: Use stock Spring annotations (@RestController, @Component, @Configuration, @Transactional, @Scheduled, @KafkaListener) + Lombok. Do NOT invent or expect framework-specific annotations like @LgController or @ApplicationService.
---

# RULE-005 — No custom framework annotations

## Statement

The lg5-spring framework does **not** ship any custom annotations such as
`@LgController`, `@ApplicationService`, `@LgEntity`, `@SagaParticipant`, or
similar. If you encounter such an annotation in code, design, or AI-generated
suggestions, treat it as **fabrication** and replace with the appropriate
stock annotation:

| Use case                          | Stock annotation                              |
|-----------------------------------|-----------------------------------------------|
| HTTP REST controller              | `@RestController` + `@RequestMapping`         |
| Spring-managed component          | `@Component` / `@Service` / `@Repository`     |
| Configuration class               | `@Configuration` + `@Bean`                    |
| Transactional method              | `@Transactional` (Spring TX)                  |
| Scheduled task                    | `@Scheduled(fixedDelayString = "...")`        |
| Kafka consumer                    | `@KafkaListener`                              |
| Conditional bean                  | `@ConditionalOnProperty` / `@Profile`         |
| Field/constructor injection       | `@RequiredArgsConstructor` (Lombok) preferred |
| Boilerplate getters/setters/builder | `@Getter`/`@Setter`/`@Builder` (Lombok)     |
| Logging                           | `@Slf4j` (Lombok)                             |

## Rationale

Custom framework annotations are sticky abstractions: they look like they
simplify the code but they hide what is really happening, lock you into the
framework version, and force every reader to learn a private DSL on top of
Spring. The lg5-spring philosophy is to compose stock Spring + Lombok
mechanically — any agent or human reader who knows Spring already knows the
codebase. The only "framework-specific" types you will encounter are
**interfaces** (`SagaStep`, `OutboxScheduler`, `KafkaProducerHelper<K, V>`)
and **classes** (`Lg5TestBoot`, `TestContainersLoader`), never annotations.

## Example — correct

```java
@RestController
@RequestMapping(value = "/payments", produces = "application/vnd.api.v1+json")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentApplicationService service;

    @PostMapping
    public PaymentResponse create(@RequestBody @Valid final PaymentCommand cmd) {
        return service.handle(cmd);
    }
}
```

```java
@Component
@RequiredArgsConstructor
public class PaymentSagaStep implements SagaStep<PaymentResponse> {
    @Override @Transactional public void process(final PaymentResponse r) { ... }
    @Override @Transactional public void rollback(final PaymentResponse r) { ... }
}
```

## Anti-pattern

```java
// WRONG: invented annotations the framework does NOT define
@LgController("/payments")
@ApplicationService
@SagaParticipant(saga = "OrderPaymentSaga")
public class PaymentController { ... }
```

If an AI suggests these, it is hallucinating from other frameworks (Axon,
Eventuate, etc.). Reject and rewrite with stock Spring.

## References

- Skill: `lg5-spring-overview` (what the framework actually provides).
- Related rules: RULE-003 (architecture), RULE-009 (saga step shape),
  RULE-010 (Kafka listener shape), RULE-011 (outbox scheduler shape).
