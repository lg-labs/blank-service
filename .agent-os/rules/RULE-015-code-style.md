---
id: RULE-015
slug: code-style
version: 0.1.0
lg5-spring-sha: d0d754a
severity: should
constitutional: false
scope: style
tags: [style, final, records, lombok, kotlin, package-layout]
description: Style defaults — `final` on locals & params; records for DTOs; Kotlin only for stateless interfaces and `@ConfigurationProperties`; package layout per concern (dto/, entity/, mapper/, event/, exception/, ports/, outbox/, saga/).
---

# RULE-015 — Code style defaults

## Statement

These conventions are `should`-level (preferred unless there's a concrete
reason not to). Reviewers may ask for justification when broken; CI does not
enforce them yet.

### Java style

- **`final` on every local and parameter** by default. Mutable locals are
  flagged in review; `final` makes refactors safer and signals intent.
- **Records for DTOs**: `ErrorDTO`, `*Command`, `*Response`, anything that
  crosses a boundary as immutable data. Use a class only when you genuinely
  need mutability (JPA entities) or inheritance.
- **Lombok**: `@RequiredArgsConstructor`, `@Slf4j`, `@Getter`, `@Builder`,
  `@Setter` — used for boilerplate, never for behavior.

### Kotlin policy

Kotlin is allowed in two scenarios only:

- **Stateless interfaces** (no companion-object state, no body — pure
  contract).
- **`@ConfigurationProperties` records** — Kotlin's `data class` + nullable
  types + default values are a great fit for the framework's
  `KafkaConfigData`, `KafkaProducerConfigData`, etc. patterns.

Kotlin business logic, Kotlin services, Kotlin entities are **not**
allowed in consumer services — keeps the codebase mono-lingual where it
matters and avoids dual JVM nightmare in stack traces.

### Package layout per concern

Inside any module that holds Spring beans, organize by **concern**, not by
type. Standard subpackages:

```
.../<concern-root>/
├── dto/                  # request/response DTOs (records)
├── entity/               # JPA entities (data-access module)
├── mapper/               # MapStruct or hand mappers between DTO ↔ domain ↔ entity
├── event/                # Kafka payload classes (in application-service module)
├── exception/            # service-specific exceptions
├── ports/
│   ├── input/            # input port interfaces (use cases)
│   │   ├── service/      # interface signatures
│   │   └── message/      # message-listener interface signatures
│   └── output/           # output port interfaces (repository, publisher)
│       ├── repository/
│       └── message/
├── outbox/
│   ├── model/            # outbox JPA + payload classes
│   └── scheduler/        # outbox scheduler implementations
└── saga/                 # SagaStep implementations
```

## Rationale

- `final` everywhere makes the code work-in-progress versus done state
  visually distinct (anything mutable jumps out) and helps the JIT.
- Records eliminate hundreds of lines of boilerplate per service while making
  immutability the default — fewer bugs from accidental mutation.
- Limiting Kotlin to stateless interfaces and config classes lets us harvest
  Kotlin's expressiveness where it shines (concise type-safe config) without
  taking on the operational cost of two JVM languages in business logic
  (mixed stack traces, build tooling complexity, hiring constraints).
- Package-by-concern co-locates everything you need to read to understand
  one feature; package-by-type ("controllers", "services", "repositories")
  scatters a single feature across N folders.

## Example — correct

```java
public record PaymentCommand(
        String customerId,
        BigDecimal amount,
        String currency
) { }

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentApplicationServiceImpl implements PaymentApplicationService {

    private final PaymentRepository repository;
    private final PaymentMapper     mapper;

    @Override
    @Transactional
    public PaymentResponse handle(final PaymentCommand command) {
        final Payment payment = mapper.toDomain(command);
        repository.save(payment);
        return mapper.toResponse(payment);
    }
}
```

```kotlin
// kafka-config.* binding
@ConfigurationProperties(prefix = "kafka-config")
data class KafkaConfigData(
    val bootstrapServers: String,
    val schemaRegistryUrl: String,
    val numOfPartitions: Int = 3,
    val replicationFactor: Short = 1
)
```

## Anti-pattern

```java
// WRONG: mutable params, no Lombok, no records, behavior in the DTO
public class PaymentCommand {
    public String customerId;
    public BigDecimal amount;
    public void normalize() { this.amount = amount.setScale(2); }   // ❌ behavior
}

public PaymentResponse handle(PaymentCommand cmd) {                 // ❌ no final
    cmd.normalize();
    Payment payment = mapper.toDomain(cmd);                         // ❌ no final
    return mapper.toResponse(payment);
}
```

```kotlin
// WRONG: Kotlin business logic in a consumer service
@Service
class PaymentService(private val repo: PaymentRepository) {
    fun handle(cmd: PaymentCommand): PaymentResponse {
        // ❌ Kotlin allowed only for interfaces and @ConfigurationProperties
    }
}
```

## References

- Skill: `lg5-spring-overview` (style conventions and module layout).
- Reference: any food-ordering-system service (`order-service`,
  `payment-service`, `restaurant-service`) demonstrates this layout.
- Related rules: RULE-005 (Lombok + stock annotations), RULE-003 (hexagonal).
