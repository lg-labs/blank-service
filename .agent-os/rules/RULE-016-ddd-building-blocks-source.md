---
id: RULE-016
slug: ddd-building-blocks-source
version: 0.1.0
lg5-spring-sha: d0d754a
severity: must
constitutional: true
scope: architecture
tags: [ddd, building-blocks, external-library, ddd-common-domain, lg5-common-domain]
description: DDD building blocks (`AggregateRoot`, `BaseEntity`, `BaseId`, `Money`, `DomainEvent`) come from the external `com.labs.lg.pentagon:ddd-common-domain` library, re-exported by `lg5-common-domain`. They are NOT defined in the lg5-spring repo.
---

# RULE-016 — DDD building blocks come from `ddd-common-domain`

## Statement

The framework-level DDD building blocks below are provided by an **external**
library, `com.labs.lg.pentagon:ddd-common-domain`, and re-exported by the
`lg5-common-domain` framework module. Always import from those packages, never
re-implement them locally:

| Building block      | Provided by package                          |
|---------------------|----------------------------------------------|
| `AggregateRoot<ID>` | `com.labs.lg.pentagon.ddd.AggregateRoot`     |
| `BaseEntity<ID>`    | `com.labs.lg.pentagon.ddd.BaseEntity`        |
| `BaseId<T>`         | `com.labs.lg.pentagon.ddd.BaseId`            |
| `Money`             | `com.labs.lg.pentagon.ddd.Money`             |
| `DomainEvent<T>`    | `com.labs.lg.pentagon.ddd.event.DomainEvent` |
| `ValueObject`       | `com.labs.lg.pentagon.ddd.ValueObject`       |

If you cannot find a class in `lg5-spring`'s source tree, check
`ddd-common-domain` first — that is by design, not a missing artifact.

## Rationale

The DDD primitives are framework-agnostic: an `AggregateRoot` should not
depend on Spring, JPA, Kafka, or anything lg5-specific. Putting them in a
shared library that pre-dates lg5-spring keeps the abstraction clean and
allows non-lg5 codebases (legacy projects, side experiments) to share the
same domain vocabulary.

Re-implementing `AggregateRoot` locally inside a service has three
consequences:

1. The service's aggregates are no longer assignment-compatible with the
   framework's helpers (the saga step expects `T extends AggregateRoot<?>`).
2. Different services drift in subtle ways — e.g. one service's `Money`
   stores `BigDecimal` + `Currency`, another's stores `long` cents, and
   crosspaying between them silently corrupts.
3. When `ddd-common-domain` evolves (added behavior on `AggregateRoot`, e.g.
   `pullDomainEvents()`), the local re-implementation diverges and the
   service can no longer pull in the new helper.

## Example — correct

```java
package com.example.payment.domain;

import com.labs.lg.pentagon.ddd.AggregateRoot;
import com.labs.lg.pentagon.ddd.Money;
import com.labs.lg.pentagon.ddd.event.DomainEvent;

public class Payment extends AggregateRoot<PaymentId> {

    private final CustomerId customerId;
    private final Money      amount;
    private       PaymentStatus status;

    public PaymentCompletedEvent complete() {
        this.status = PaymentStatus.COMPLETED;
        return new PaymentCompletedEvent(this);
    }
}

public record PaymentCompletedEvent(Payment payment, ZonedDateTime occurredAt)
        implements DomainEvent<Payment> { }
```

```java
public class PaymentId extends BaseId<UUID> {
    public PaymentId(final UUID value) { super(value); }
}
```

`pom.xml`:

```xml
<dependency>
  <groupId>com.lg5.spring</groupId>
  <artifactId>lg5-common-domain</artifactId>
</dependency>
<!-- ddd-common-domain is brought in transitively; do NOT add it directly -->
```

## Anti-pattern

```java
// WRONG: re-implement AggregateRoot locally
package com.example.payment.domain;

public abstract class AggregateRoot<ID> {                  // ❌
    private ID id;
    public ID getId() { return id; }
    // missing pullDomainEvents() etc. — diverges from the framework
}

public class Payment extends AggregateRoot<PaymentId> { ... }   // not lg5-compatible
```

```java
// WRONG: re-implement Money as primitive
public class Money {
    private final long cents;            // ❌ different from framework's BigDecimal+Currency
    private final String currencyCode;
}
```

## References

- External library: https://github.com/lg-labs-pentagon/ddd-common-domain
  (re-exported by `lg5-common-domain`).
- Skill: `lg5-spring-overview` (module map showing `lg5-common-domain` as the
  re-export point).
- Related rules: RULE-003 (hexagonal+DDD architecture), RULE-004 (module shape).
